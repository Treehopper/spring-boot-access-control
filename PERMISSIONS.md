# Permissions

<!-- Generated from the policy by PermissionsDocumentTest. Do not edit by hand: regenerate with
     ./mvnw test -Dtest=PermissionsDocumentTest -Dpermissions.update -->

Who may do what in the homeowners' portal. Anything not listed here is not allowed.
A relationship only counts while it lasts: when a lease ends, a flat is sold or a delegation is
revoked, the permissions that came with it end immediately.

## Overview

|  | Owners | Tenants | Landlords | District managers | Senders | Addressees | Account holders | Delegates |
|---|:---:|:---:|:---:|:---:|:---:|:---:|:---:|:---:|
| See the association and its notices | ✔ | ✔ |  | ✔ |  |  |  |  |
| See who owns which unit | ✔ |  |  | ✔ |  |  |  |  |
| See how many tenants live in the association | ✔ |  |  | ✔ |  |  |  |  |
| See who a tenant is (name and unit) |  |  | ✔ | ✔ |  |  |  |  |
| See which units nobody lives in |  |  |  | ✔ |  |  |  |  |
| See polls and their results | ✔ |  |  | ✔ |  |  |  |  |
| Open a poll |  |  |  | ✔ |  |  |  |  |
| Vote in a poll | ✔ |  |  |  |  |  |  |  |
| Send messages to residents |  |  |  | ✔ |  |  |  |  |
| Read a message |  |  |  | ✔ | ✔ | ✔ |  |  |
| Admit residents, change their role and end their access |  |  |  | ✔ |  |  |  |  |
| See a dues account |  |  |  | ✔ |  |  | ✔ | ✔ |
| Decide who else may see a dues account |  |  |  | ✔ |  |  | ✔ |  |

## Relationships

### Owners

People who currently own a unit in the association, whether they live there or rent it out.

They may:

- see the association and its notices
- see who owns which unit
- see how many tenants live in the association
- see polls and their results
- vote in a poll

### Tenants

People who currently rent a unit in the association.

They may:

- see the association and its notices

### Landlords

The owner of the unit a tenant rents. This only relates an owner to their own tenants.

They may:

- see who a tenant is (name and unit)

### District managers

Staff currently responsible for the district the association belongs to, such as its property manager.

They may:

- see the association and its notices
- see who owns which unit
- see how many tenants live in the association
- see who a tenant is (name and unit)
- see which units nobody lives in
- see polls and their results
- open a poll
- send messages to residents
- read a message
- admit residents, change their role and end their access
- see a dues account
- decide who else may see a dues account

### Senders

Whoever sent the message.

They may:

- read a message

### Addressees

Residents a message is addressed to, judged by their current role: everyone, owners, people living there, tenants, or one person.

They may:

- read a message

### Account holders

The owner a dues account belongs to.

They may:

- see a dues account
- decide who else may see a dues account

### Delegates

People the account holder currently shares their dues account with, such as their accountant.

They may:

- see a dues account
