# Spring Boot access control demo: a homeowners' association

A Spring Boot 4 application that shows **relationship-based access control** with Spring Security method
security. Whether you may do something depends on how you relate to the thing, and that relationship can end.
Owners vote and see the member list. Tenants only receive notices. A property manager runs every association
in their district. An accountant may see one owner's dues account.

## Run it

```bash
./mvnw package          # builds and runs the tests
java -jar target/spring-boot-access-control-0.0.1-SNAPSHOT.jar
```

Open <http://localhost:8080> and pick a persona. There is no password, and you can switch personas at any
time. Data lives in memory and is reset on restart.

Most units are rented out by their owner to a tenant. mia lives in her own flat, and Apt 3 has been vacant since
erin moved out:

| Association | Unit | Owner | Tenant |
|---|---|---|---|
| Maple Court (Riverside) | Apt 1 | alice | bob |
| | Apt 2 | frank | kim |
| | Apt 3 | frank | vacant (erin's lease ended) |
| | Apt 4 | mia | — (mia lives there herself) |
| Oak Terrace (Riverside) | No. 7 | grace | heidi |
| Birch Hill (Hillside) | House 2 | judy | leo |

Living in the same unit doesn't mean having the same rights:

| Persona | What they get |
|---|---|
| alice, frank, grace, judy, mia (owners) | notices and owner messages, member list with the names of their own tenants (others only as a count), polls and voting, own dues |
| bob, kim, heidi, leo (tenants) | notices to everyone, to those living here or to tenants, personal messages; no member list, polls or owner's dues |
| carol (property manager, Riverside) | everything in Maple Court and Oak Terrace, sending messages, managing residents and units |
| ivan (property manager, Hillside) | the same for Birch Hill; carol and ivan can't see each other's district |
| dave (Alice's accountant) | Alice's dues account only |
| erin (former tenant) | nothing, until carol reinstates her in Apt 3 |

As carol you can send a message to everyone, to owners only, to everyone living there (tenants and owner-occupiers), to tenants only, or to one resident. On the
*Residents* tab you can make bob an owner, end someone's access or reinstate erin. Then switch persona to see
the effect. The *Access check* tab sends real requests and shows which ones the server allows for the current
persona.

## How it works

**Policy.** All rules are in one place,
[`HomeownersPolicy`](src/main/java/eu/hohenegger/accesscontrol/permission/HomeownersPolicy.java):

```java
Policy.define()
        .allow(VIEW_ASSOCIATION).to(residents(), districtManagers())
        .allow(VIEW_MEMBERS).to(owners(), districtManagers())
        .allow(VIEW_TENANT).to(landlords(), districtManagers().of(Membership::association))
        .allow(VOTE).to(owners().of(Poll::association))
        .allow(READ_MESSAGE).to(theSender(), addressees(), districtManagers().of(Message::association))
        .allow(VIEW_DUES).to(theAccountHolder(), delegates(), managersOfTheHolder())
        // ...
        .build();
```

Anything not allowed is denied. Every condition only counts relationships that are active right now, so an
ended membership or delegation takes effect immediately.

**Controllers** use a meta-annotation with placeholders (Spring Security's `AnnotationTemplateExpressionDefaults`):

```java
@PostMapping("/polls/{pollId}/votes")
@RequirePermission(action = "VOTE", on = "pollId")
PollView vote(@PathVariable String pollId, @RequestBody Ballot ballot, User me) { ... }
```

**In code**, the same policy is available through a fluent API. It is used for filtering lists and for
field-level decisions, such as which tenants an owner may see by name:

```java
permissions.check(me).may(VIEW_TENANT).filter(association.residents().activeAt(now).tenants().list())
permissions.check(me).may(READ_MESSAGE).filter(community.messages())
```

**Domain.** The demo data is built with the same style
([`DemoData`](src/main/java/eu/hohenegger/accesscontrol/demo/DemoData.java)):

```java
maple.unit("Apt 1").ownedBy(alice).rentedTo(bob);
maple.unit("Apt 3").ownedBy(frank).rentedTo(erin).since(now.minus(400, DAYS)).until(now.minus(30, DAYS));
community.grant(dave).accessToDuesOf(alice).as("Accountant");
community.message().from(carol).toOwnersOf(maple).about("Annual assembly: agenda").saying("...");
```

**Audit log.** All permission-related activity goes to the logger `access`. At `DEBUG` it also logs every
single policy decision.

```
SESSION visitor now acts as bob (Tenant · Maple Court, Apt 1)
DENIED  bob VIEW_MEMBERS on Maple Court (is not owner or district manager)
GRANTED dave VIEW_DUES on alice (Accountant for Alice Andersen)
FILTERED bob READ_MESSAGE: 4 of 7 granted
ADMIN   carol ended bob's access to Maple Court
```

**Tests.** The `@WebMvcTest` tests state, for each endpoint, exactly who may use it. Every other persona is
expected to get `403`, so each line generates one positive or negative test per persona (677 tests in total):

```java
access("read a message to the owners of Maple Court", () -> get("/api/messages/{id}", messageId(ASSEMBLY)))
        .grantedOnlyTo("alice", "carol", "frank");
```

Requests for ids that don't exist are also answered with `403`, not `404`, so the API doesn't reveal what exists.

## Design notes

### Demo domain: a university course

An **auditor** attends and sees the material, but isn't on the roster and doesn't hand in work or
vote: a silent participant. An **enrolled student** is the open participant.


#### Demo data

| User | Role | `GET /api/courses` | Roster | Grades of Alice | Poll |
|-------|---|---|---|---|---|
| alice | enrolled in Astronomy 101 | Astronomy 101 | ✔ (auditors shown as a count) | ✔ | ✔ |
| bob | auditor in Astronomy 101 | Astronomy 101 | ✘ | ✘ | ✘ |
| carol | lecturer, physics department | all physics courses | ✔ (with names) | ✔ | — |
| dave | Alice's guardian | — | ✘ | ✔ | ✘ |
| erin | guest lecturer, expired | — | ✘ | ✘ | ✘ |

### Other demo domains considered

| Domain | Fits well | Weaker spot |
|---|---|---|
| **Homeowners' association / community garden** | Members vote, tenants (silent) see notices only | Less familiar internationally; few documents |

#### Homeowners' association: demo data

| User | Role | `GET /api/associations` | Member list | Dues account of Alice | Vote |
|-------|---|---|---|---|---|
| alice | owner-member of Maple Court | Maple Court | ✔ (tenants shown as a count) | ✔ | ✔ |
| bob | tenant in Maple Court | Maple Court | ✘ | ✘ | ✘ |
| carol | property manager, Riverside district | all Riverside associations | ✔ (with names) | ✔ | — |
| dave | Alice's accountant | — | ✘ | ✔ | ✘ |
| erin | former tenant, lease ended | — | ✘ | ✘ | ✘ |
