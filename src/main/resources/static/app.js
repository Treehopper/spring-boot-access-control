'use strict';

// --- helpers ----------------------------------------------------------------------------------------------------

/** Creates an element; children may be strings, nodes, arrays or null. Text is never parsed as HTML. */
function h(tag, attrs = {}, ...children) {
    const element = document.createElement(tag);
    for (const [key, value] of Object.entries(attrs)) {
        if (value === null || value === undefined || value === false) continue;
        if (key.startsWith('on')) element.addEventListener(key.slice(2), value);
        else if (key === 'class') element.className = value;
        else element.setAttribute(key, value === true ? '' : value);
    }
    for (const child of children.flat(Infinity)) {
        if (child === null || child === undefined || child === false) continue;
        element.append(child instanceof Node ? child : String(child));
    }
    return element;
}

const $ = selector => document.querySelector(selector);
const initials = name => name.split(' ').map(part => part[0]).join('').slice(0, 2).toUpperCase();
const date = iso => new Date(iso).toLocaleDateString(undefined, {year: 'numeric', month: 'short', day: 'numeric'});
const money = cents => (cents / 100).toLocaleString(undefined, {style: 'currency', currency: 'EUR'});

function toast(text, kind = '') {
    const element = h('div', {class: `toast ${kind}`}, text);
    $('#toasts').append(element);
    setTimeout(() => element.remove(), 4000);
}

class ApiError extends Error {
    constructor(status, message) {
        super(message);
        this.status = status;
    }
}

function csrfToken() {
    const cookie = document.cookie.split('; ').find(entry => entry.startsWith('XSRF-TOKEN='));
    return cookie ? decodeURIComponent(cookie.split('=')[1]) : '';
}

/** Calls the API. Errors are shown as toasts unless {silent: true}. */
async function api(method, path, body, {silent = false} = {}) {
    const headers = {};
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    if (method !== 'GET') headers['X-XSRF-TOKEN'] = csrfToken();
    const response = await fetch(path, {
        method, headers, credentials: 'same-origin',
        body: body === undefined ? undefined : JSON.stringify(body),
    });
    if (response.status === 401 && !silent) {
        openPersonaDialog();
        throw new ApiError(401, 'Choose a persona first');
    }
    if (!response.ok) {
        let message = response.status === 403 ? 'Access denied' : `Request failed (${response.status})`;
        if (response.status !== 403) {
            try {
                const problem = await response.json();
                if (problem.message || problem.detail) message = problem.message || problem.detail;
            } catch (ignored) { /* no JSON body */ }
        }
        if (!silent) toast(message, 'error');
        throw new ApiError(response.status, message);
    }
    return response.status === 204 ? null : response.json();
}

// API errors are already shown as toasts; don't report them again as unhandled.
window.addEventListener('unhandledrejection', event => {
    if (event.reason instanceof ApiError) event.preventDefault();
});

// --- state ------------------------------------------------------------------------------------------------------

const state = {
    me: null,
    associationId: null,
    tab: null,
};

const currentAssociation = () => state.me?.associations.find(a => a.id === state.associationId) ?? null;

function tabsFor(me, association) {
    const can = association?.can ?? {};
    return [
        association && {id: 'messages', label: 'Messages'},
        can.viewMembers && {id: 'members', label: 'Members'},
        can.viewPolls && {id: 'polls', label: 'Polls'},
        me.duesAccounts.length > 0 && {id: 'dues', label: me.duesAccounts.length === 1 && me.duesAccounts[0].username === me.username ? 'My dues' : 'Dues accounts'},
        can.sendMessage && {id: 'compose', label: 'Send message'},
        can.manageResidents && {id: 'residents', label: 'Residents'},
        {id: 'check', label: 'Access check'},
    ].filter(Boolean);
}

// --- persona selection ------------------------------------------------------------------------------------------

/** Personas grouped by association and unit; people who live nowhere (managers, the accountant) come last. */
async function openPersonaDialog() {
    const dialog = $('#persona-dialog');
    if (dialog.open) return;
    const personas = await api('GET', '/api/personas');
    const associations = new Map();
    const elsewhere = [];
    for (const persona of personas) {
        if (persona.homes.length === 0) elsewhere.push(persona);
        for (const home of persona.homes) {
            if (!associations.has(home.associationId)) {
                associations.set(home.associationId, {name: home.association, district: home.district, units: new Map()});
            }
            const units = associations.get(home.associationId).units;
            if (!units.has(home.unitId)) units.set(home.unitId, {name: home.unit, entries: []});
            units.get(home.unitId).entries.push({persona, home});
        }
    }
    const rank = ({home}) => (home.current ? 0 : 2) + (home.role === 'OWNER' ? 0 : 1);
    const roleText = home => !home.current ? `Former ${home.role.toLowerCase()}`
        : home.role === 'TENANT' ? 'Tenant' : home.livesThere ? 'Owner · lives here' : 'Owner · rents it out';
    const button = (persona, subtitle) => h('button', {type: 'button', class: 'persona', onclick: () => choosePersona(persona.username)},
        h('div', {class: 'avatar'}, initials(persona.displayName)),
        h('div', {}, h('strong', {}, persona.displayName), h('small', {}, subtitle)));

    $('#persona-list').replaceChildren(
        ...[...associations.values()].map(association => h('section', {class: 'persona-group'},
            h('h3', {}, association.name, ' ', h('small', {class: 'muted'}, `${association.district} district`)),
            [...association.units.values()]
                .sort((a, b) => a.name.localeCompare(b.name, undefined, {numeric: true}))
                .map(unit => h('div', {class: 'unit-row'},
                    h('div', {class: 'unit-name'}, unit.name),
                    h('div', {class: 'unit-people'},
                        unit.entries.sort((a, b) => rank(a) - rank(b)).map(({persona, home}) => button(persona, roleText(home)))))))),
        elsewhere.length > 0 && h('section', {class: 'persona-group'},
            h('h3', {}, 'Management and others'),
            h('div', {class: 'unit-people'}, elsewhere.map(persona => button(persona, persona.description)))));
    dialog.showModal();
}

$('#persona-dialog').addEventListener('cancel', event => {
    if (!state.me) event.preventDefault(); // someone has to be chosen
});

async function choosePersona(username) {
    await api('POST', '/api/session', {username});
    $('#persona-dialog').close();
    state.associationId = null;
    state.tab = null;
    await load();
}

$('#switch').addEventListener('click', openPersonaDialog);

// --- page -------------------------------------------------------------------------------------------------------

async function load() {
    try {
        state.me = await api('GET', '/api/me');
    } catch (error) {
        if (error.status === 401) return;
        throw error;
    }
    const me = state.me;
    $('#who').hidden = false;
    $('#who-avatar').textContent = initials(me.displayName);
    $('#who-name').textContent = me.displayName;
    $('#who-description').textContent = me.description;
    $('#who-description').title = me.description;

    if (!me.associations.some(a => a.id === state.associationId)) {
        state.associationId = me.associations[0]?.id ?? null;
    }
    render();
}

function render() {
    const me = state.me;
    const app = $('#app');

    if (me.associations.length === 0 && me.duesAccounts.length === 0) {
        app.replaceChildren(h('div', {class: 'card empty'},
            h('div', {class: 'avatar'}, initials(me.displayName)),
            h('h2', {}, 'Nothing to see here right now'),
            h('p', {class: 'muted'}, me.description),
            h('p', {class: 'muted'}, 'You have no current relationship with any association, so the portal shows nothing. ' +
                'Ask a property manager to reinstate you.'),
            h('button', {class: 'secondary', onclick: () => selectTab('check')}, 'Run access check')));
        if (state.tab === 'check') renderTabInto(app, me, null, 'check');
        return;
    }

    const association = currentAssociation();
    const tabs = tabsFor(me, association);
    if (!tabs.some(tab => tab.id === state.tab)) state.tab = tabs[0].id;

    const context = h('div', {class: 'context'},
        association
            ? h('div', {},
                h('h1', {}, association.name),
                h('div', {class: 'muted'}, `${association.district} district · `, relationBadge(association.relation)))
            : h('div', {}, h('h1', {}, 'Dues accounts'), h('div', {class: 'muted'}, me.description)),
        me.associations.length > 1 && h('select', {
                'aria-label': 'Association',
                onchange: event => { state.associationId = event.target.value; render(); },
            },
            me.associations.map(a => h('option', {value: a.id, selected: a.id === state.associationId}, `${a.name} (${a.district})`))));

    const tabBar = h('div', {class: 'tabs', role: 'tablist'},
        tabs.map(tab => h('button', {
            role: 'tab', 'aria-selected': String(tab.id === state.tab), onclick: () => selectTab(tab.id),
        }, tab.label)));

    const panel = h('section', {class: 'stack', role: 'tabpanel'});
    app.replaceChildren(context, tabBar, panel);
    renderTabInto(panel, me, association, state.tab);
}

function selectTab(id) {
    state.tab = id;
    render();
}

function relationBadge(relation) {
    const kind = relation === 'Owner' ? 'owner' : relation === 'Tenant' ? 'tenant' : 'staff';
    return h('span', {class: `badge ${kind}`}, relation);
}

async function renderTabInto(panel, me, association, tab) {
    const renderers = {messages, members, polls, dues, compose, residents, check};
    panel.append(h('p', {class: 'muted'}, 'Loading…'));
    try {
        const content = await renderers[tab](me, association);
        panel.lastChild.remove();
        panel.append(...[content].flat());
    } catch (error) {
        panel.lastChild.replaceWith(h('div', {class: 'notice'}, error.message));
    }
}

// --- tabs -------------------------------------------------------------------------------------------------------

async function messages(me, association) {
    const inbox = (await api('GET', '/api/messages')).filter(message => message.association === association.id);
    if (inbox.length === 0) return h('div', {class: 'notice'}, 'No messages for you in this association.');
    return inbox.map(message => h('article', {class: 'card message'},
        h('h3', {}, message.subject),
        h('div', {class: 'meta'},
            h('span', {}, `From ${message.from.displayName}`),
            h('span', {class: `badge ${message.audience === 'INDIVIDUAL' ? 'personal' : ['TENANTS', 'OCCUPANTS'].includes(message.audience) ? 'tenant' : message.audience === 'OWNERS' ? 'owner' : 'staff'}`}, message.audienceLabel),
            h('span', {}, date(message.sentAt))),
        h('p', {}, message.body)));
}

async function members(me, association) {
    const list = await api('GET', `/api/associations/${association.id}/members`);
    const people = (members, kind) => h('ul', {class: 'people'}, members.map(member =>
        h('li', {}, h('div', {class: 'avatar'}, initials(member.displayName)),
            h('div', {}, h('strong', {}, member.displayName),
                h('small', {}, kind === 'owner' ? `${member.unit} · ${member.livesThere ? 'lives here' : 'rents it out'}` : member.unit)),
            h('span', {class: `badge ${kind}`, style: 'margin-left:auto'}, kind === 'owner' ? 'Owner' : 'Tenant'))));
    return [
        h('div', {class: 'card'}, h('h2', {}, `Owners (${new Set(list.owners.map(owner => owner.username)).size})`), people(list.owners, 'owner')),
        h('div', {class: 'card'}, h('h2', {}, `Tenants (${list.tenantCount})`),
            list.tenantCount === 0 && h('p', {class: 'muted'}, 'No tenants.'),
            list.tenants.length > 0 && people(list.tenants, 'tenant'),
            list.tenantCount > list.tenants.length && h('div', {class: 'notice', style: list.tenants.length ? 'margin-top:8px' : ''},
                list.tenants.length
                    ? `You see the tenants of your own flats. ${list.tenantCount - list.tenants.length} more tenant(s) live here; only their landlord and property management see their names.`
                    : `${list.tenantCount} tenant(s) live here. Only their landlord and property management see their names.`)),
    ];
}

async function polls(me, association) {
    const all = await api('GET', `/api/associations/${association.id}/polls`);
    const cards = all.map(poll => {
        const total = Object.values(poll.results).reduce((sum, count) => sum + count, 0);
        return h('article', {class: 'card'},
            h('h3', {}, poll.question),
            h('div', {class: 'muted'}, `Asked by ${poll.askedBy.displayName} · ${total} vote(s)`),
            h('div', {class: 'results'}, Object.entries(poll.results).map(([choice, count]) =>
                h('div', {class: `result ${poll.myVote === choice ? 'mine' : ''}`},
                    h('span', {}, choice.charAt(0) + choice.slice(1).toLowerCase()),
                    h('div', {class: 'bar'}, h('span', {style: `width:${total ? (100 * count / total) : 0}%`})),
                    h('span', {class: 'num'}, count)))),
            poll.canVote
                ? h('div', {class: 'row'},
                    h('span', {class: 'muted'}, poll.myVote ? `You voted ${poll.myVote.toLowerCase()}. Change:` : 'Your vote:'),
                    ['YES', 'NO', 'ABSTAIN'].map(choice => h('button', {
                        class: `small ${poll.myVote === choice ? '' : 'secondary'}`,
                        onclick: async () => { await api('POST', `/api/polls/${poll.id}/votes`, {choice}); toast('Vote recorded'); render(); },
                    }, choice.charAt(0) + choice.slice(1).toLowerCase())))
                : h('div', {class: 'notice'}, 'Only owners vote. You can follow the results.'));
    });
    if (association.can.createPoll) {
        const question = h('input', {placeholder: 'e.g. Replace the front door?', required: true});
        cards.unshift(h('form', {
                class: 'card', onsubmit: async event => {
                    event.preventDefault();
                    await api('POST', `/api/associations/${association.id}/polls`, {question: question.value});
                    toast('Poll opened');
                    render();
                },
            },
            h('h3', {}, 'Open a new poll'),
            h('div', {class: 'row'}, question, h('button', {}, 'Open poll'))));
    }
    if (all.length === 0 && !association.can.createPoll) return h('div', {class: 'notice'}, 'No polls yet.');
    return cards;
}

let duesHolder = null;

async function dues(me) {
    if (!me.duesAccounts.some(account => account.username === duesHolder)) {
        duesHolder = me.duesAccounts.find(account => account.username === me.username)?.username ?? me.duesAccounts[0].username;
    }
    const account = await api('GET', `/api/residents/${duesHolder}/dues`);
    const picker = me.duesAccounts.length > 1 && h('label', {},
        h('span', {}, 'Account'),
        h('select', {onchange: event => { duesHolder = event.target.value; render(); }},
            me.duesAccounts.map(holder => h('option', {value: holder.username, selected: holder.username === duesHolder}, holder.displayName))));

    const sheet = h('div', {class: 'card'},
        picker,
        h('div', {class: 'row', style: 'justify-content:space-between'},
            h('h2', {}, `Dues of ${account.holder.displayName}`),
            h('div', {}, h('small', {class: 'muted'}, 'Balance '), h('span', {class: 'balance'}, money(account.balanceCents)))),
        account.entries.length === 0
            ? h('p', {class: 'muted'}, 'No entries.')
            : h('div', {class: 'table-wrap'}, h('table', {},
                h('thead', {}, h('tr', {}, h('th', {}, 'Date'), h('th', {}, 'Description'), h('th', {class: 'num'}, 'Amount'))),
                h('tbody', {}, account.entries.map(entry => h('tr', {},
                    h('td', {}, date(entry.date)),
                    h('td', {}, entry.description),
                    h('td', {class: `num ${entry.amountCents < 0 ? 'amount-negative' : ''}`}, money(entry.amountCents))))))));

    if (account.sharedWith === null) return sheet;

    const delegateName = h('input', {placeholder: 'username, e.g. dave', required: true, autocapitalize: 'none', autocorrect: 'off', spellcheck: 'false'});
    const purpose = h('input', {placeholder: 'purpose, e.g. Accountant'});
    return [sheet, h('div', {class: 'card'},
        h('h2', {}, 'Shared with'),
        account.sharedWith.length === 0
            ? h('p', {class: 'muted'}, 'Nobody else can see this account.')
            : h('ul', {class: 'people'}, account.sharedWith.map(shared => h('li', {},
                h('div', {class: 'avatar'}, initials(shared.delegate.displayName)),
                h('div', {}, h('strong', {}, shared.delegate.displayName), h('small', {}, `${shared.purpose} · since ${date(shared.since)}`)),
                h('button', {
                    class: 'danger small', style: 'margin-left:auto',
                    onclick: async () => {
                        await api('DELETE', `/api/admin/residents/${account.holder.username}/delegates/${shared.delegate.username}`);
                        toast(`${shared.delegate.displayName} can no longer see this account`);
                        render();
                    },
                }, 'Revoke')))),
        h('form', {
                class: 'row', style: 'margin-top:12px', onsubmit: async event => {
                    event.preventDefault();
                    await api('PUT', `/api/admin/residents/${account.holder.username}/delegates/${encodeURIComponent(delegateName.value.trim())}`, {purpose: purpose.value});
                    toast('Account shared');
                    render();
                },
            },
            delegateName, purpose, h('button', {}, 'Share')))];
}

async function compose(me, association) {
    const residents = (await api('GET', `/api/admin/associations/${association.id}/residents`)).filter(resident => resident.active);
    const audience = h('select', {name: 'audience'},
        h('option', {value: 'EVERYONE'}, `Everyone in ${association.name}`),
        h('option', {value: 'OWNERS'}, 'Owners only'),
        h('option', {value: 'OCCUPANTS'}, 'Everyone living here (tenants and owners who live here)'),
        h('option', {value: 'TENANTS'}, 'Tenants only'),
        h('option', {value: 'INDIVIDUAL'}, 'One resident'));
    const recipient = h('select', {name: 'recipient'},
        residents.map(resident => h('option', {value: resident.person.username},
            `${resident.person.displayName} (${resident.role.toLowerCase()}, ${resident.unit})`)));
    const recipientField = h('label', {hidden: true}, h('span', {}, 'Recipient'), recipient);
    audience.addEventListener('change', () => { recipientField.hidden = audience.value !== 'INDIVIDUAL'; });
    const subject = h('input', {required: true, maxlength: 120});
    const body = h('textarea', {required: true});

    return h('form', {
            class: 'card', onsubmit: async event => {
                event.preventDefault();
                await api('POST', `/api/associations/${association.id}/messages`, {
                    audience: audience.value,
                    recipient: audience.value === 'INDIVIDUAL' ? recipient.value : null,
                    subject: subject.value, body: body.value,
                });
                toast('Message sent');
                selectTab('messages');
            },
        },
        h('h2', {}, 'New message'),
        h('label', {}, h('span', {}, 'To'), audience),
        recipientField,
        h('label', {}, h('span', {}, 'Subject'), subject),
        h('label', {}, h('span', {}, 'Message'), body),
        h('button', {}, 'Send'));
}

async function residents(me, association) {
    const [all, summary] = await Promise.all([
        api('GET', `/api/admin/associations/${association.id}/residents`),
        api('GET', `/api/associations/${association.id}`)]);
    const change = async (resident, role) => {
        await api('PUT', `/api/admin/associations/${association.id}/units/${resident.unitId}/residents/${resident.person.username}`, {role});
        toast(`${resident.person.displayName} is now ${role.toLowerCase()} of ${resident.unit}`);
        render();
    };
    const reinstate = async resident => {
        await api('PUT', `/api/admin/associations/${association.id}/units/${resident.unitId}/residents/${resident.person.username}`,
            {role: resident.role, livesThere: resident.livesThere});
        toast(`${resident.person.displayName} is back in ${resident.unit} as ${resident.role.toLowerCase()}`);
        render();
    };
    const activeAgain = resident => all.some(other => other.active
        && other.unitId === resident.unitId && other.person.username === resident.person.username);
    const end = async resident => {
        await api('DELETE', `/api/admin/associations/${association.id}/units/${resident.unitId}/residents/${resident.person.username}`);
        toast(`${resident.person.displayName}'s access to ${resident.unit} has ended`);
        render();
    };
    return h('div', {class: 'card'},
        h('h2', {}, 'Residents'),
        h('p', {class: 'muted'}, 'Changes take effect immediately. Switch persona to see the result through their eyes.'),
        summary.vacantUnits.length > 0 && h('div', {class: 'notice', style: 'margin-bottom:12px'},
            `Nobody lives in: ${summary.vacantUnits.join(', ')}`),
        h('div', {class: 'table-wrap'}, h('table', {},
            h('thead', {}, h('tr', {}, h('th', {}, 'Unit'), h('th', {}, 'Name'), h('th', {}, 'Role'), h('th', {}, 'Status'), h('th', {}, ''))),
            h('tbody', {}, all.map(resident => h('tr', {class: resident.active ? '' : 'ended'},
                h('td', {}, resident.unit),
                h('td', {}, resident.person.displayName),
                h('td', {}, h('span', {class: `badge ${resident.active ? resident.role.toLowerCase() : 'ended'}`}, resident.role === 'OWNER' ? 'Owner' : 'Tenant'),
                    resident.role === 'OWNER' && resident.livesThere && h('small', {class: 'muted'}, ' lives here')),
                h('td', {}, resident.active ? `since ${date(resident.since)}` : `ended ${date(resident.until)}`),
                h('td', {class: 'num'}, h('div', {class: 'row', style: 'justify-content:flex-end'},
                    resident.active
                        ? [h('button', {class: 'secondary small', onclick: () => change(resident, resident.role === 'OWNER' ? 'TENANT' : 'OWNER')},
                            resident.role === 'OWNER' ? 'Make tenant' : 'Make owner'),
                           h('button', {class: 'danger small', onclick: () => end(resident)}, 'End access')]
                        : activeAgain(resident)
                            ? h('small', {class: 'muted'}, 'reinstated')
                            : h('button', {class: 'secondary small', onclick: () => reinstate(resident)}, 'Reinstate')))))))));
}

/** Asks the server directly, so the answers are what the policy says, not what the UI chose to show. */
async function check(me) {
    // deliberately includes associations this persona cannot see
    const associations = {'maple-court': 'Maple Court', 'oak-terrace': 'Oak Terrace', 'birch-hill': 'Birch Hill'};
    const probes = [
        ...Object.entries(associations).flatMap(([id, name]) => [
            {label: `${name}: view association`, path: `/api/associations/${id}`},
            {label: `${name}: member list`, path: `/api/associations/${id}/members`},
            {label: `${name}: polls`, path: `/api/associations/${id}/polls`},
            {label: `${name}: manage residents`, path: `/api/admin/associations/${id}/residents`},
        ]),
        {label: 'Dues account of Alice', path: '/api/residents/alice/dues'},
        {label: 'Dues account of Frank', path: '/api/residents/frank/dues'},
    ];
    const results = await Promise.all(probes.map(probe =>
        api('GET', probe.path, undefined, {silent: true}).then(() => 200, error => error.status)));
    return h('div', {class: 'card'},
        h('h2', {}, `What the server lets ${me.displayName} read`),
        h('p', {class: 'muted'}, 'Each line is a real request made just now. Denied requests are logged as warnings.'),
        h('div', {class: 'check-grid'}, probes.map((probe, index) => {
            const granted = results[index] === 200;
            return h('div', {class: `check ${granted ? 'granted' : 'denied'}`},
                h('span', {class: 'icon', 'aria-label': granted ? 'granted' : 'denied'}, granted ? '✔' : '✘'),
                h('div', {}, probe.label, ' ', h('small', {}, h('code', {}, `GET ${probe.path}`))),
                h('small', {}, results[index]));
        })));
}

// --- start ------------------------------------------------------------------------------------------------------

load();
