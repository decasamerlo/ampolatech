# AmpolaTech

Software for compounding pharmacies that make sterile injectables and hormone pellets to order, each against a doctor's prescription.

## Language

### People

**Pharmacy**:
A compounding pharmacy that uses AmpolaTech. Every user works for exactly one pharmacy.
_Avoid_: Client, customer, org, tenant

**User**:
A member of a pharmacy's staff who signs in to AmpolaTech. Their roles set what they may do.
_Avoid_: Account, login

**Actor**:
The user a request acts for, as far as the request needs them: who they are, the pharmacy they work for, and their roles.
_Avoid_: Principal

**Role**:
A named set of permissions a user holds in their pharmacy, such as sales agent or pharmacist. It is often named after a job, but needn't match the jobs its holder does.
_Avoid_: Permission, job, profile (a false friend of _perfil_), access level

**Doctor**:
The physician who wrote a prescription. Doctors don't use AmpolaTech; they send their prescriptions to a sales agent over WhatsApp.
_Avoid_: User, customer

**Sales agent**:
A pharmacy employee who receives prescriptions from doctors over WhatsApp and registers orders from them. Called _vendedor_ in Portuguese for now.
_Avoid_: Attendant, seller, representative

**Pharmacist**:
A licensed pharmacist on a pharmacy's staff. Only a pharmacist adds or changes master formulas.
_Avoid_: Chemist, technician

**Patient**:
The person an order is prepared for. Every order names exactly one patient.
_Avoid_: Customer, client, user

### Orders

**Prescription**:
The document a doctor wrote, as the sales agent received it. A prescription can be registered without an order.
_Avoid_: Script, Rx, receipt (a false friend of _receita_)

**Order**:
The pharmacy's record of what to prepare for one patient. An order may carry the prescription it was registered from.
_Avoid_: Request, sale, purchase

**Order item**:
One formula from an order's prescription, made from one master formula. Each order item is prepared on its own.
_Avoid_: Line, product

### Formulas

**Master formula**:
A preparation the pharmacy has standardized: its components, dosage form, and a shelf life backed by stability data. Each strength is its own master formula. A formula a doctor prescribes can be ordered only once a master formula exists for it.
_Avoid_: Recipe, template, product
