# Reference software for AmpolaTech

Date: 2026-10-20

**Summary.** Read [OpenMRS](#openmrs) for the prescription-to-dispense model and its FHIR mapping, and [Odoo's manufacturing and lot-expiry modules with OCA's quality control](#odoo-community-with-oca-quality-control) (or [ERPNext](#erpnext)) for production orders that consume and produce lots and pass an inspection. For workflow, study [Fagron Tech's FórmulaCerta and RXmais](#fagron-tech-fórmulacerta-phusion-rxmais-tech_ia) (the Brazilian baseline, including a prescriber platform), [LifeFile's provider portal](#lifefile) (ordering limited to prescribers), and [Baxter DoseEdge](#baxter-doseedge) with [Seal's 503B blueprint](#seal-503b-blueprint) (sterile preparation, quarantine, release). No open-source project models compounding, and no Brazilian vendor page found mentions sterile injectables, so the sterile release workflow has to come from the US references and from [RDC 67/2007 Annex IV](#regulation-pointers-not-researched-here).

How to read this file:

- Every claim links to the source it was checked against on 2026-10-20: a repo file, a vendor page, or the standard's own spec. Where only a secondary source exists, the text says so.
- Code links point at each repo's default or stable branch as of that date, so line numbers can drift.
- Reading code is free. Copying it into AmpolaTech brings that license's obligations: AGPL-3.0, GPL, MPL-2.0 (per file), EPL-1.0 or LGPL-3.0, as listed per entry. A repo with no LICENSE file grants no reuse rights: "[nobody else can copy, distribute, or modify your work](https://choosealicense.com/no-permission/)".

## 1. Open source

No open-source compounding system turned up. GitHub searches for "compounding pharmacy", "farmácia de manipulação", "sterile compounding", "IV compounding" and "electronic batch record" return pharmacy websites, student projects and demos, almost all without a license (see [Dropped](#dropped)). What the open-source world offers are parts: a prescription and dispense model (OpenMRS), production orders with lot genealogy (Odoo, ERPNext), lot ledgers (OpenLMIS, OpenBoxes) and QC lab results (SENAITE).

| Project | License | Stack | Last activity | Read it for |
| --- | --- | --- | --- | --- |
| OpenMRS core, Dispensing app, Stock Management, FHIR2 | MPL-2.0 (Stock Management: no LICENSE file) | Java/Hibernate; TypeScript/React | commits 2026-10-01 to 2026-10-20 | order and fulfiller status, dispense record, batch choice by expiry |
| Odoo Community + OCA `quality_control_oca` | LGPL-3.0; AGPL-3.0 | Python, PostgreSQL | Odoo 19.0: 2026-10-20; OCA module: 2026-07-23 | production order, lot traceability, expiry, QC inspection states |
| ERPNext | GPL-3.0 | Python (Frappe), MariaDB | release v16.51.1, 2026-10-15 | make-to-order chain, batch expiry, quality inspection, retained samples |
| OpenLMIS Stock Management | AGPL-3.0 | Java/Spring | 2026-10-16 | stock ledger: events, reasons, signatures per lot |
| OpenBoxes | EPL-1.0 | Groovy/Grails | release v0.9.8-hotfix1, 2026-08-14 | lot status (quarantine, recall) |
| OpenEMR | GPL-3.0 | PHP, MySQL | release v8_4_1, 2026-09-20 | lot destruction with witness, one movement table |
| SENAITE | GPL-2.0 | Python (Plone) | release v2.6.0, 2025-04-04; commits to 2026-10-20 | QC specs and multiple-verification of results |

### OpenMRS

**What it is.** An open-source medical record system. Since version 2.6 its core has a [`MedicationDispense`](https://github.com/openmrs/openmrs-core/blob/master/api/src/main/java/org/openmrs/MedicationDispense.java) entity whose Javadoc points to the FHIR MedicationDispense resource. A separate frontend, the [Dispensing app](https://github.com/openmrs/openmrs-esm-dispensing-app), is the pharmacist's screen.

**Why it matters for AmpolaTech.** It is the most complete open model of "a prescriber orders, the pharmacy fulfils and records what it handed over". It also shows where a model built for manufactured drugs stops fitting a pharmacy that makes each preparation to order.

- **License, stack, activity.** Core: [MPL-2.0](https://github.com/openmrs/openmrs-core/blob/master/LICENSE), Java with Hibernate, last commit 2026-10-20, [release 2.8.10](https://github.com/openmrs/openmrs-core/releases/tag/2.8.10) on 2026-10-06. Dispensing app: [MPL-2.0](https://github.com/openmrs/openmrs-esm-dispensing-app/blob/main/LICENSE), TypeScript/React, last commit 2026-10-13 (tag v1.11.2). [Stock Management module](https://github.com/openmrs/openmrs-module-stockmanagement): Java, last commit 2026-10-20, **no LICENSE file in the repo**, so reuse terms are unverified. [FHIR2 module](https://github.com/openmrs/openmrs-module-fhir2): MPL-2.0, last commit 2026-10-01.
- **Paths worth reading.**
  - [`Order.java`](https://github.com/openmrs/openmrs-core/blob/master/api/src/main/java/org/openmrs/Order.java#L67-L96): an order has an `orderer` (a Provider), an `Action` (`NEW`, `REVISE`, `DISCONTINUE`, `RENEW`), a `previousOrder` that chains revisions, `commentToFulfiller`, and a `FulfillerStatus` the pharmacy sets (`RECEIVED`, `IN_PROGRESS`, `EXCEPTION`, `ON_HOLD`, `DECLINED`, `COMPLETED`) with a `fulfillerComment`. This is a ready vocabulary for "the doctor's order" and "what the pharmacy is doing with it" as two separate state machines.
  - [`DrugOrder.java`](https://github.com/openmrs/openmrs-core/blob/master/api/src/main/java/org/openmrs/DrugOrder.java): dose, units, frequency, route, duration, quantity, `numRefills`, `dispenseAsWritten`, and `drugNonCoded` for a drug outside the formulary.
  - [`Drug.java`](https://github.com/openmrs/openmrs-core/blob/master/api/src/main/java/org/openmrs/Drug.java) and [`DrugIngredient.java`](https://github.com/openmrs/openmrs-core/blob/master/api/src/main/java/org/openmrs/DrugIngredient.java): a formulary entry with dosage form and a list of ingredients (concept, strength, units). It is a catalog item; there is no per-patient preparation and no batch.
  - [`MedicationDispense.java`](https://github.com/openmrs/openmrs-core/blob/master/api/src/main/java/org/openmrs/MedicationDispense.java): patient, the `drugOrder` it fulfils, `dispenser`, coded `status` and `statusReason`, quantity, dose, route, frequency, `datePrepared`, `dateHandedOver`, and substitution type and reason. The entity is `@Audited` (Hibernate Envers history).
  - Dispensing app, [`medication-dispense.resource.tsx`](https://github.com/openmrs/openmrs-esm-dispensing-app/blob/main/src/medication-dispense/medication-dispense.resource.tsx#L14-L30): saves dispenses as FHIR `MedicationDispense` through the FHIR2 REST API, and has a TODO that sets `whenPrepared` equal to `whenHandedOver`. AmpolaTech cannot do that: a sterile preparation waits for test results between the two.
  - Dispensing app, [`stock-dispense.component.tsx`](https://github.com/openmrs/openmrs-esm-dispensing-app/blob/main/src/forms/stock-dispense/stock-dispense.component.tsx#L18-L76): offers only batches that will not expire before the end of the therapy (computed from the dosage duration) and sorts them by earliest expiry. RDC 67/2007 asks for the same link between shelf life and treatment period ([item 15.4.1](https://bvsms.saude.gov.br/bvs/saudelegis/anvisa/2007/rdc0067_08_10_2007.html)).
  - Dispensing app [README](https://github.com/openmrs/openmrs-esm-dispensing-app#required-privileges): separate privileges to dispense, to dispense with changed quantity or formulation, to edit, to delete, and to delete only one's own dispense. A usable starting split for pharmacy roles.
  - Stock Management [model package](https://github.com/openmrs/openmrs-module-stockmanagement/tree/master/api/src/main/java/org/openmrs/module/stockmanagement/api/model): [`StockItem`](https://github.com/openmrs/openmrs-module-stockmanagement/blob/master/api/src/main/java/org/openmrs/module/stockmanagement/api/model/StockItem.java) (`hasExpiration`, `expiryNotice`, dispensing unit, packaging units), [`StockBatch`](https://github.com/openmrs/openmrs-module-stockmanagement/blob/master/api/src/main/java/org/openmrs/module/stockmanagement/api/model/StockBatch.java) (`batchNo`, `expiration`, `expiryNotificationDate`), [`TransactionBase`](https://github.com/openmrs/openmrs-module-stockmanagement/blob/master/api/src/main/java/org/openmrs/module/stockmanagement/api/model/TransactionBase.java) (quantity per batch per operation, optional patient), [`StockOperation`](https://github.com/openmrs/openmrs-module-stockmanagement/blob/master/api/src/main/java/org/openmrs/module/stockmanagement/api/model/StockOperation.java) (who submitted, dispatched, completed, rejected or returned it, and when).
  - FHIR2, [`MedicationTranslatorImpl.java`](https://github.com/openmrs/openmrs-module-fhir2/blob/master/api/src/main/java/org/openmrs/module/fhir2/api/translators/impl/MedicationTranslatorImpl.java#L55-L60): maps each ingredient's item to FHIR but not its strength. [`MedicationRequestTranslatorImpl.java`](https://github.com/openmrs/openmrs-module-fhir2/blob/master/api/src/main/java/org/openmrs/module/fhir2/api/translators/impl/MedicationRequestTranslatorImpl.java#L137-L141) carries `FulfillerStatus` as a FHIR extension. Both show where the core model and FHIR differ.
- **Limits.** Everything assumes a manufactured drug picked from a formulary. There is no formula, no compounding record and no QC release.

### Odoo Community with OCA quality control

**What it is.** An ERP. The community edition ships manufacturing (`mrp`), inventory (`stock`) and lot expiry (`product_expiry`, `mrp_product_expiry`). Odoo's own quality module is not in the community repo (the [19.0 addons](https://github.com/odoo/odoo/tree/19.0/addons) have no `quality*` module), so the Odoo Community Association's [`quality_control_oca`](https://github.com/OCA/manufacture/tree/18.0/quality_control_oca) fills that gap.

**Why it matters for AmpolaTech.** A manufacturing order that consumes component lots and produces a finished lot has the same shape as a compounding batch. Odoo also builds upstream and downstream lot traceability, which is what a recall needs.

- **License, stack, activity.** Odoo Community is [LGPL-3.0](https://github.com/odoo/odoo/blob/19.0/LICENSE), Python and PostgreSQL; branch 19.0 had its last commit on 2026-10-20, and a [20.0 branch](https://github.com/odoo/odoo/tree/20.0) also exists. `quality_control_oca` is [AGPL-3](https://github.com/OCA/manufacture/blob/18.0/quality_control_oca/__manifest__.py); on branch 18.0 its last commit was 2026-07-23. Branch 18.0 also has [`quality_control_mrp_oca`](https://github.com/OCA/manufacture/tree/18.0/quality_control_mrp_oca) and [`quality_control_stock_oca`](https://github.com/OCA/manufacture/tree/18.0/quality_control_stock_oca); branch 19.0 has only `quality_control_oca` so far.
- **Paths worth reading.**
  - [`mrp/models/mrp_bom.py`](https://github.com/odoo/odoo/blob/19.0/addons/mrp/models/mrp_bom.py#L14-L42): bill of materials with component lines and quantity. It is the closest analog to a master formula.
  - [`mrp/models/mrp_production.py`](https://github.com/odoo/odoo/blob/19.0/addons/mrp/models/mrp_production.py#L120-L213): the manufacturing order, with `lot_producing_ids`, `move_raw_ids` (components consumed) and `move_finished_ids`, and states `draft`, `confirmed`, `progress`, `to_close`, `done`, `cancel`.
  - [`mrp/models/stock_traceability.py`](https://github.com/odoo/odoo/blob/19.0/addons/mrp/models/stock_traceability.py) extends [`stock/report/stock_traceability.py`](https://github.com/odoo/odoo/blob/19.0/addons/stock/report/stock_traceability.py): the lot traceability report across production.
  - [`product_expiry/models/production_lot.py`](https://github.com/odoo/odoo/blob/19.0/addons/product_expiry/models/production_lot.py#L50-L56): a lot's `expiration_date` is the time the lot is created plus the product's fixed `expiration_time` in days ([field help](https://github.com/odoo/odoo/blob/19.0/addons/product_expiry/models/product_product.py#L36-L56)); best-before, removal and alert dates are offsets from it.
  - [`mrp_product_expiry/models/mrp_production.py`](https://github.com/odoo/odoo/blob/19.0/addons/mrp_product_expiry/models/mrp_production.py#L10-L31): closing an order that consumed an expired lot opens a confirmation wizard. It warns; it does not block.
  - OCA [`qc_inspection.py`](https://github.com/OCA/manufacture/blob/18.0/quality_control_oca/models/qc_inspection.py#L70-L79): inspection states `draft`, `ready`, `waiting` (supervisor approval), `success`, `failed`, `canceled`. The [README](https://github.com/OCA/manufacture/blob/18.0/quality_control_oca/README.rst) describes qualitative and quantitative questions grouped into tests, attached to products or categories by triggers.
- **What does not fit.** A fixed shelf life per product and a warn-only expiry check are wrong for sterile compounding. Under RDC 67/2007 the shelf life comes from stability data ([item 15.4](https://bvsms.saude.gov.br/bvs/saudelegis/anvisa/2007/rdc0067_08_10_2007.html)). AmpolaTech will need its own rule, checked against the component lots actually used (analysis, not a quoted requirement).

### ERPNext

**What it is.** A Frappe-based ERP with batches, manufacturing and quality inspection. It is [GPL-3.0](https://github.com/frappe/erpnext/blob/develop/license.txt), Python on Frappe with MariaDB; [release v16.51.1](https://github.com/frappe/erpnext/releases/tag/v16.51.1) came out on 2026-10-15.

**Why it matters for AmpolaTech.** It has the full make-to-order chain in one model: a customer order drives a work order, the work order produces a batch, and the batch passes an inspection before delivery. It also inspects raw materials on receipt and keeps retained samples.

- **Paths worth reading.**
  - [`stock/doctype/batch/batch.json`](https://github.com/frappe/erpnext/blob/develop/erpnext/stock/doctype/batch/batch.json): `manufacturing_date`, `expiry_date`, `parent_batch`, and `reference_doctype`/`reference_name` for the document that created the batch. [`batch.py`](https://github.com/frappe/erpnext/blob/develop/erpnext/stock/doctype/batch/batch.py#L237-L245) sets expiry to manufacturing date plus the item's `shelf_life_in_days`, which has the same fixed-shelf-life limit as Odoo.
  - [`stock/doctype/item/item.json`](https://github.com/frappe/erpnext/blob/develop/erpnext/stock/doctype/item/item.json): `has_batch_no`, `has_expiry_date`, `retain_sample` with `sample_quantity`, `inspection_required_before_purchase`, `inspection_required_before_delivery`, `quality_inspection_template`.
  - [`stock/doctype/quality_inspection/quality_inspection.json`](https://github.com/frappe/erpnext/blob/develop/erpnext/stock/doctype/quality_inspection/quality_inspection.json): type (`Incoming`, `Outgoing`, `In Process`), a reference to the receipt, delivery, stock entry or job card, `batch_no`, `inspected_by`, `verified_by`, readings against a template, and status `Accepted`, `Rejected` or `Cancelled`.
  - [`manufacturing/doctype/work_order/work_order.json`](https://github.com/frappe/erpnext/blob/develop/erpnext/manufacturing/doctype/work_order/work_order.json): links to `sales_order` and `bom_no` and has its own status list. [`bom.json`](https://github.com/frappe/erpnext/blob/develop/erpnext/manufacturing/doctype/bom/bom.json) carries `inspection_required` and a QC template.

### OpenLMIS Stock Management

**What it is.** The stock service of a public-health supply-chain system. It is [AGPL-3.0](https://github.com/OpenLMIS/openlmis-stockmanagement/blob/master/LICENSE), Java/Spring; its last commit was on 2026-10-16 (tag v5.4.2).

**Why it matters for AmpolaTech.** It is a stock ledger in which every movement is an event with a reason, a document number and a signature, kept per product and lot. Brazil's controlled-substance bookkeeping (SNGPC) asks for exactly this: every entry and exit, including losses and "transformações", per lot ([RDC 22/2014](#sngpc-controlled-substance-bookkeeping)).

- **Paths worth reading.** [`domain/event/StockEvent.java`](https://github.com/OpenLMIS/openlmis-stockmanagement/blob/master/src/main/java/org/openlmis/stockmanagement/domain/event/StockEvent.java) (document number, signature, line items); [`domain/card/StockCard.java`](https://github.com/OpenLMIS/openlmis-stockmanagement/blob/master/src/main/java/org/openlmis/stockmanagement/domain/card/StockCard.java) (one card per facility, program, product and lot); [`domain/card/StockCardLineItem.java`](https://github.com/OpenLMIS/openlmis-stockmanagement/blob/master/src/main/java/org/openlmis/stockmanagement/domain/card/StockCardLineItem.java) (quantity, reason, source, destination, separate occurred and processed dates, signature, resulting stock on hand); [`domain/reason/`](https://github.com/OpenLMIS/openlmis-stockmanagement/tree/master/src/main/java/org/openlmis/stockmanagement/domain/reason) (reason types and categories); [`domain/physicalinventory/`](https://github.com/OpenLMIS/openlmis-stockmanagement/tree/master/src/main/java/org/openlmis/stockmanagement/domain/physicalinventory); and in the reference-data service, [`Lot.java`](https://github.com/OpenLMIS/openlmis-referencedata/blob/master/src/main/java/org/openlmis/referencedata/domain/Lot.java) (lot code, expiration date, manufacture date, trade item).

### OpenBoxes

**What it is.** A warehouse and inventory system for health facilities. It is [EPL-1.0](https://github.com/openboxes/openboxes/blob/develop/LICENSE.md), Groovy on Grails; [release v0.9.8-hotfix1](https://github.com/openboxes/openboxes/releases/tag/v0.9.8-hotfix1) came out on 2026-08-14 and commits continue (2026-10-20).

**Why it matters for AmpolaTech.** It treats lot status as a first-class field, which is what quarantine and recall of raw-material lots and finished preparations need.

- **Paths worth reading.** [`InventoryItem.groovy`](https://github.com/openboxes/openboxes/blob/develop/grails-app/domain/org/pih/warehouse/inventory/InventoryItem.groovy) (a lot is product + lot number, unique per product, with expiration date and `lotStatus`); [`LotStatusCode.groovy`](https://github.com/openboxes/openboxes/blob/develop/src/main/groovy/org/pih/warehouse/inventory/LotStatusCode.groovy) (`APPROVED`, `RECALLED`, `ON_HOLD`, `QUARANTINED`, `EXPIRED`, `RESERVED`, `DAMAGED`); `Transaction.groovy` and `TransactionEntry.groovy` in the [same folder](https://github.com/openboxes/openboxes/tree/develop/grails-app/domain/org/pih/warehouse/inventory).

### OpenEMR

**What it is.** A practice-management and medical-record system with a small dispensary. It is [GPL-3.0](https://github.com/openemr/openemr/blob/master/LICENSE), PHP and MySQL; [release v8_4_1](https://github.com/openemr/openemr/releases/tag/v8_4_1) came out on 2026-09-20.

**Why it matters for AmpolaTech.** Two details only. Its lot table records destruction with date, method and witness, which matters when expired or rejected controlled lots are destroyed. And a single table records every stock movement by type.

- **Paths worth reading.** [`sql/database.sql`, table `drug_inventory`](https://github.com/openemr/openemr/blob/master/sql/database.sql#L1520-L1535) (lot number, expiration, manufacturer, on hand, warehouse, `destroy_date`, `destroy_method`, `destroy_witness`, `destroy_notes`); [table `drug_sales`](https://github.com/openemr/openemr/blob/master/sql/database.sql#L1544-L1574) (`trans_type` 1 sale, 2 purchase, 3 return, 4 transfer, 5 adjustment, linked to prescription and lot); [table `prescriptions`](https://github.com/openemr/openemr/blob/master/sql/database.sql#L8714-L8768); the screens in [`interface/drugs/`](https://github.com/openemr/openemr/tree/master/interface/drugs) (`add_edit_lot.php`, `destroy_lot.php`, `dispense_drug.php`).

### SENAITE

**What it is.** An open-source laboratory information system. It is [GPL-2.0](https://github.com/senaite/senaite.core/blob/2.x/LICENSE), Python on Plone; the last [release, v2.6.0](https://github.com/senaite/senaite.core/releases/tag/v2.6.0), came out on 2025-04-04, and commits continue (2026-10-20).

**Why it matters for AmpolaTech.** Only if AmpolaTech records its own QC lab results (sterility, endotoxin, assay). It shows result specifications as ranges and a configurable number of independent verifications per result.

- **Paths worth reading.** [`bika/lims/content/bikasetup.py`](https://github.com/senaite/senaite.core/blob/2.x/src/bika/lims/content/bikasetup.py#L486-L520) (`SelfVerificationEnabled`, `NumberOfRequiredVerifications`); [`bika/lims/content/analysisspec.py`](https://github.com/senaite/senaite.core/blob/2.x/src/bika/lims/content/analysisspec.py#L86) (`ResultsRange`).

### Brief mentions

- **Bahmni** combines OpenMRS with Odoo. Its [Odoo 16 modules](https://github.com/Bahmni/bahmni-odoo-modules) are [MPL-2.0](https://github.com/Bahmni/bahmni-odoo-modules/blob/master/LICENSE), with the last commit on 2026-10-07. The only part worth reading is [`bahmni_api_feed/models/order_save_service.py`](https://github.com/Bahmni/bahmni-odoo-modules/blob/master/bahmni_api_feed/models/order_save_service.py#L146-L165), which turns OpenMRS drug orders into Odoo sale orders and splits dispensed from not-yet-dispensed orders. It shows one way to hand an order from the clinical side to the stock side.
- **GNU Health HIS** is a hospital system on Tryton, [GPL-3.0](https://codeberg.org/gnuhealth/his/src/branch/devel/tryton/COPYING), with a commit on 2026-10-12 and release 5.0.7 on 2026-04-11. Its [`health_stock`](https://codeberg.org/gnuhealth/his/src/branch/devel/tryton/health_stock/README.rst) package adds an expiration date to lots and creates pharmacy stock moves from a prescription ([`health_stock.py`](https://codeberg.org/gnuhealth/his/src/branch/devel/tryton/health_stock/health_stock.py)). It covers the same ground as OpenMRS, with less detail.
- **Marley Health** (formerly `frappe/health`, now [`earthians/marley`](https://github.com/earthians/marley)) is [GPL-3.0](https://github.com/earthians/marley/blob/develop/license.txt), with the last commit on 2026-10-07. Its [`medication_request`](https://github.com/earthians/marley/blob/develop/healthcare/healthcare/doctype/medication_request/medication_request.json) doctype follows FHIR (status, intent, priority, practitioner, repeats allowed, total dispensable quantity) on top of ERPNext. It is relevant only if AmpolaTech builds on Frappe.

### Dropped

- **OCA `vertical-medical`** (medical prescription and pharmacy modules for Odoo): the modules exist only on the [10.0 branch](https://github.com/OCA/vertical-medical/tree/10.0), last commit 2021-10-16; the newer branches are empty.
- **Compounding-specific GitHub repos:** none is substantial. Examples: [`pauloedsfer/quality_control`](https://github.com/pauloedsfer/quality_control) (a single HTML file for a Brazilian compounding pharmacy's QC that stores data in the browser's localStorage, no license); [`wsingleton/sekhmet`](https://github.com/wsingleton/sekhmet) (test reports for USP <797> cleanrooms, last push 2019, no license); [`frogalewski/sistema-pharma-comphusion`](https://github.com/frogalewski/sistema-pharma-comphusion) (an AI Studio demo, two commits).

## 2. Commercial products

### Brazil

#### Fagron Tech: FórmulaCerta, Phusion, RXmais, Tech_IA

Fagron Tech belongs to the Fagron group. The group's page credits Fagron Tech with "[4000+](https://fagron.com/fagron-group/fagron-tech/)" pharmacies in Brazil and 30 years of experience, and names only Phusion as a product.

- **FórmulaCerta** (desktop ERP). The [product page](https://fagrontech.com.br/solucoes/formulacerta) covers purchasing, sales and CRM, and "obrigações fiscais e sanitárias". It also lists an integrated drug database with "milhares de fichas de especificações técnicas" from Fagron Solutions, "Pesagem Monitorada" (monitored weighing), quotes, customizable labels (a homologated printer is required), and a "rótulo digital inteligente": a QR code on the label opens the formula's posology, lot and expiry. The page says nothing about SNGPC, sterile products, a QC module or the compounding record itself.
- **Phusion** (cloud ERP). The [product page](https://fagrontech.com.br/solucoes/phusion) calls it "um ERP Online" with "Em Nuvem" and mobile access. It lists labels, "visitação médica" (scheduling visits to prescribers and keeping their data), CRM, dashboards and a support chatbot.
- **RXmais** (prescriber platform). The [product page](https://fagrontech.com.br/solucoes/rxmais) describes an e-prescribing portal for compounded formulas with "mais de 8 mil fórmulas magistrais", aimed at doctors, nutritionists and pharmacists. Prescriptions are signed with an ICP-Brasil certificate, and each one carries a QR code that "permite a inclusão direta no FórmulaCerta, sem a necessidade de digitação". This is the Brazilian precedent for AmpolaTech's rule that only doctors place orders, and the most useful Fagron Tech product to study.
- **Tech_IA** (prescription reading). The [product page](https://fagrontech.com.br/solucoes/tech-ia) says it "lê as prescrições e preenche automaticamente todas as informações no sistema", turning a prescription into a quote. An attendant reviews the result before it is sent. It reads prescriptions that arrive by WhatsApp, works in FórmulaCerta and Phusion, and is sold by credits, one per prescription file.

Status of the leads from the earlier session:

| Lead | Verdict |
| --- | --- |
| FórmulaCerta is the Brazilian market leader | **Vendor claim only.** The vendor's site calls FórmulaCerta "[O sistema mais usado nas farmácias de manipulação do Brasil](https://www.fagrontech.com.br/)" and "[líder de mercado](https://fagrontech.com.br/solucoes/formulacerta)". A partner case study reports "[mais de 3500 clientes](https://notagateway.com.br/cases/fagron-tech/)". No independent market-share figure was found. |
| Phusion is the newer cloud product | **Cloud: verified** ([vendor page](https://fagrontech.com.br/solucoes/phusion)). **"Newer": secondary only.** The only source is a partner case study calling it "[recém-lançado](https://notagateway.com.br/cases/fagron-tech/)"; it is undated, and it mentions a contract signed in September 2020. No launch date on the vendor's pages. |
| Fagron Tech added AI prescription reading in 2025 | **Feature: verified** ([Tech_IA page](https://fagrontech.com.br/solucoes/tech-ia)). **Year: secondary only.** A trade-press article dated [15/09/2025](https://www.baguete.com.br/noticias/ia-agora-le-receitas) says Fagron Tech "acaba de agregar" the feature, built with ChatGPT and the firm Impulso, and processing 50,000 prescriptions a month in 200 pharmacies. The vendor page gives no date. |

#### Other Brazilian compounding software

These vendor pages describe compounding in general terms. They are useful as a checklist of what the market treats as basic.

| Product (vendor) | What its own page claims | Source |
| --- | --- | --- |
| Pharmacie, Manipulação Humana (Prime Software, Goiânia) | "Pesagem monitorada, Peso médio"; "Rastreabilidade de lotes e processos"; "Controle de qualidade com assinatura digital"; "Emissão de livros e geração do SNGPC"; NF-e, NFS-e, NFC-e, SPED | [page](https://primesoftware.com.br/sistema/pharmacie-manipulacao-humana) |
| FarmaFácil Manipulação (Prismafive, "desde 1993") | Pesagem Monitorada, Peso Médio, Fichas Técnicas; "Rastreabilidade Total"; one SNGPC file for compounding plus drugstore; NF-e import | [page](https://prismafive.com.br/farmafacil-manipulacao/) |
| OuroFórmulas (VSM Informática, Assis-SP) | "fórmulas padrão"; "Integração com balanças de precisão… com registro em tempo real"; stage control "desde o recebimento de matéria-prima até o produto final"; lot traceability "até a dispensação ao paciente"; QC checks at every stage; SNGPC files; prescription entry with automated validation; NFS-e | [page](https://www.vsm.com.br/ouroformulas/) |
| Medicator / Medicator One (TEXS) | "Ordens de manipulação, rotulagem e controle de qualidade embutidos no fluxo", citing RDC 67/2007; per-lot tracking of raw materials and finished products, with expiry alerts and sale blocking; SNGPC; "EasyFarma IA" reads handwritten and digital prescriptions to build a quote | [page](https://www.texs.com.br/) |
| Vitória Farmácia (VitóriaSoft, Serra-ES) | weighing that checks "quantidade e lote de cada insumo" and records "o tempo de operação e o responsável pela pesagem"; average weight with statistical sample control per the Farmacopeia Brasileira; lots and expiry; SNGPC; an AI assistant that "interpreta a receita" and sends a quote | [page](https://vitoriasoft.com.br/) |

Not checked: MEDDIX (Acabit), because its site did not resolve on 2026-10-20. A search-engine excerpt of its page describes per-formula status "Triagem, Pesagem, Encapsulamento/Manipulação, Pré-conferência, Controle de qualidade e Pronta", but this is unverified. Intuictive, HOS Sistemas and Pharma Control appeared in search results but were not opened.

**Gap.** None of these vendor pages, Fagron Tech's included, mentions sterile or injectable preparations. The QC features they name specifically are monitored weighing and average weight ("peso médio"), which are checks for oral solids. Sterility, endotoxin, quarantine and release have to come from the international references and from RDC 67/2007 Annex IV.

### International

#### Sterile preparation workflow

##### Baxter DoseEdge

**What it does.** DoseEdge is a hospital IV and oral-liquid dose-preparation workflow. Baxter's [FAQ (June 2014)](https://catalog.baxter.com/medias/DoseEdge-System-Frequently-Asked-Questions.pdf?context=bWFzdGVyfHJvb3R8NTQ0MTExfGFwcGxpY2F0aW9uL3BkZnxhRFEyTDJnMFlpOHhNREl5TkRnMU56a3dOekl6TUM5RWIzTmxSV1JuWlNCVGVYTjBaVzBnUm5KbGNYVmxiblJzZVNCQmMydGxaQ0JSZFdWemRHbHZibk11Y0dSbXxjYjUyYjViNThhYzVmMmE2ZGYzYmEyOTYyMTM5Zjk2MzJiNmI5YTJlMDM0ZmRmNjAwOWVhZGFjMjE1MzYwYThk) gives the steps:

1. A technician works an electronic dose queue by priority.
2. The technician follows on-screen instructions, scans each barcoded container to confirm it may be used, and captures images of the compounding.
3. The technician quarantines the finished dose, products and materials "until verified by a pharmacist".
4. The pharmacist inspects remotely from a web CheckStation, using the images.
5. Status boards show each dose's state (queuing, preparation, approval, distribution).
6. A verification label is applied, and the dose is tracked outside the pharmacy by barcode.

Orders reach DoseEdge either by emulating the label printer of the hospital's pharmacy system or through HL7. Routing rules decide which PrepStation may prepare which doses (for example, non-hazardous only). It handles both patient-specific and stock doses.

**Why it matters for AmpolaTech.** It is a complete prepare, quarantine, inspect and release loop for sterile doses, with photographic evidence. Baxter's current product pages refused automated access (HTTP 403 and a healthcare-professional gate), so features newer than 2014 were not checked.

##### Pestle

**What it does.** Pestle sells two products ([rxpestle.com](https://rxpestle.com); the domain pestle.io only redirects to an empty landing page):

- **Compliance** ([page](https://rxpestle.com/compliance/)): SOP management, staff training assignments, cleaning schedules, environmental-monitoring logs, equipment calibration, quality checks, investigation tracking, facility certifications, and records described as time-stamped and uneditable.
- **Formulas** ([page](https://rxpestle.com/formulas/)): an IV workflow system with "Master Formulation Records", a "Centralized Compounding Log", barcode scanning, photo capture, "Patient-specific or Batch preparations", approval checklists with "independent double-checks", and a "Complete Audit Log".

The vendor states alignment with USP <795>, <797> and <800>. Its pages do not mention 503A or 503B, BUD assignment, or lot capture. A partner listing places the company in [Denver](https://mtec-sc.org/life-sciences/pestle).

**Why it matters for AmpolaTech.** It is the clearest list of the cleanroom compliance records a sterile compounder keeps, separate from the preparation record.

##### Simplifi 797 (Wolters Kluwer)

**Partly verified.** Wolters Kluwer's pages blocked automated fetches, so the following comes from search-engine excerpts of [wolterskluwer.com pages](https://www.wolterskluwer.com/en/solutions/solesource/simplifi-797), not a full reading: daily, weekly and monthly compliance tasks with reminders, automated environmental monitoring through a Sonicu integration, surface-sample tracking, and competency schedules for aseptic manipulation and garbing. It is the same category as Pestle Compliance.

Not checked, same category as DoseEdge: BD Cato and Omnicell IVX.

#### Manufacturing-grade (503B outsourcing facilities)

##### Seal 503B blueprint

**What it does.** Seal (operated by Opvia Ltd) sells a general GxP platform. Its [503B compounding blueprint](https://seal.run/blueprints/503b-compounding) covers:

- electronic batch records, with scanned components and calculations from verified formulas;
- environmental monitoring against alert and action limits;
- deviations and CAPA;
- QC release, where "release waits for required results" and the quality unit reviews the record before authorizing the batch;
- beyond-use dating, whose basis is configured in the product definition and linked to stability and container-closure evidence;
- USP <71> sterility and <85> endotoxin tests linked to batches;
- lot traceability from components to distribution;
- electronic signatures recording who signed, when and with what meaning (the page cites 21 CFR 11.50);
- media fills, training, equipment and document control.

**Why it matters for AmpolaTech.** Two ideas to take: release is blocked by the system until the required results exist, and a BUD is a property of the formula backed by stability evidence, not a number typed per batch.

#### 503A compounding systems with prescriber portals

##### LifeFile

**What it does.** LifeFile calls itself "[a leading pharmacy management system for both specialty and compounding pharmacies](https://www.lifefile.net/)" and has four portals: pharmacy, provider, patient and salesperson.

- The [provider portal](https://www.lifefile.net/provider-portal) offers "True compound e-prescribing services with option for practice customized compound formularies", EPCS for Schedule II to V substances, "Real time order status for ALL orders sent to the pharmacy", and reports.
- The [pharmacy portal](https://www.lifefile.net/pharmacy-portal) lists integrated shipping, "PDMP Reporting Automation" and a "Quality Assurance Module" that is not described.

Batches, lots and BUD are not described on these pages.

**Why it matters for AmpolaTech.** It is the reference for a portal where doctors order compounds from the pharmacy's own formulary and then follow each order's status.

##### SiCompounding

**What it does.** SiCompounding (Isak Computing, built on Claris FileMaker) lists, on its [homepage](https://www.sicompounding.io):

- a physician portal where "Physicians will prescribe compounding medications using your formulary", plus a patient portal;
- integrated e-prescribing with EPCS certification by Drummond;
- "Automated PDMP Reporting to 50 States";
- QC with "OOS and CAPA Report" and "Vendor Qualification";
- FedEx, USPS and UPS integration.

Its [Claris Marketplace listing](https://marketplace.claris.com/detail/2562.html) (updated 2023-11-01) adds Surescripts certification, DEA 21 CFR Part 1311 certification, a B2B order API, and a price from $250 per user per month. Master formulas, BUD and lot capture are not on these pages.

##### Compound Direct

**What it does.** Compound Direct is "[Australian-built software](https://compound.direct/)" for compounding pharmacies. Balance integration lets staff "Validate and record each measurement". Labels "will be checked to make sure it matches the compound". It has checklists, a separate checking stage, custom pre-dispense checks, batch selection for stock rotation, and photos attached to the compounding record. The page does not mention sterile work or expiry dating.

#### Retail pharmacy systems with compounding add-ons (low relevance)

These systems serve US retail pharmacies. Their compounding support exists mainly to bill multi-ingredient claims, so there is little here for a made-to-order sterile manufacturer.

- **PioneerRx.** A [2018 post](https://www.pioneerrx.com/blog/featured-feature-batches) says users can "build and bill compounds without the need for any third-party add-ons" and sort batches by batch or lot number.
- **PrimeRx.** The [compounding page](https://www.primerx.io/pharmacy-compounding) lists automatic deduction of the ingredient quantities used in each compound, flags for expired stock, NDC verification of every component, a record of "the exact blend of drugs dispensed", and temperature sensors with alerts.
- **BestRx.** The [Compound help page](https://bestrxps.atlassian.net/wiki/spaces/BRXHLP/pages/883392702/Compound) (updated 2026-05-28) covers compound details required by the NCPDP D.0 claim standard. It rescales ingredients while preserving ratios when the dispensed quantity differs from the built quantity, and has fields for lot number, lot expiration date and serial number.
- **Liberty Software.** Dropped: its [homepage](https://www.libertysoftware.com/) and [feature page](https://libertysoftware.com/popular-features) do not describe compounding.

## 3. Standards and data models

### HL7 FHIR (medication resources)

The current published FHIR version is [R5 (5.0.0)](https://hl7.org/fhir/).

- **[Medication](https://hl7.org/fhir/R5/medication.html)** puts compounding in scope: "The Medication resource can be used to describe a compounded (aka extemporaneous or magistral) product that is manufactured by the pharmacy at the time of dispensing", with multiple ingredients that "are typically base chemicals". Each `ingredient` has `item`, `isActive` and `strength[x]`; `batch` has `lotNumber` and `expirationDate`; there are also `doseForm` and `totalVolume`.
- **[MedicationKnowledge](https://hl7.org/fhir/R5/medicationknowledge.html)** is the catalog or formulary view of a medication. It includes `storageGuideline.stabilityDuration`, a candidate home for a formula's stability-based BUD.
- **[MedicationRequest](https://hl7.org/fhir/R5/medicationrequest.html)** is the doctor's order. It carries `requester`, `intent`, `status`, `dosageInstruction`, and `dispenseRequest` (`validityPeriod`, `numberOfRepeatsAllowed`, `quantity`, `dispenser`), plus `substitution`. It allows only one medication per request; a prescription with several items uses several requests sharing a `groupIdentifier`.
- **[MedicationDispense](https://hl7.org/fhir/R5/medicationdispense.html)** has statuses including `preparation`, `in-progress` and `completed`, plus `authorizingPrescription`, `performer` (with `function`), `whenPrepared` and `whenHandedOver`, and `substitution`.

A possible mapping (analysis, to test against the domain model): master formula → MedicationKnowledge; one prepared batch → Medication with ingredients and `batch`; the doctor's order → MedicationRequest; release and hand-over → MedicationDispense.

Brazil's national health data network publishes an [RNDS implementation guide](https://rnds-fhir.saude.gov.br/) based on FHIR 4.0.1 (generated 2022-12-14). Its [artifacts list](https://rnds-fhir.saude.gov.br/artifacts.html) had no medication, prescription or dispense profile on 2026-10-20.

### SNGPC (controlled-substance bookkeeping)

- **Scope.** [RDC 22/2014](https://bvsms.saude.gov.br/bvs/saudelegis/anvisa/2014/rdc0022_29_04_2014.pdf) makes every pharmacy and drugstore use SNGPC for the "escrituração" of controlled medicines, "insumos farmacêuticos e preparações" (arts. 1 to 3).
  - An exit can be a sale, loss, transfer or "transformação de medicamentos, insumos farmacêuticos e produtos manipulados acabados" (definition XXVII).
  - For raw materials, "deve ser escriturado o número do lote do fabricante" (art. 10 §5).
  - Files go out at intervals of 1 to 7 days, even without movement (art. 10 §3).
  - This copy of the text, from the Ministry of Health's legislation library, warns that it does not replace the official gazette (DOU).
- **Transmission paused and resumed.** [RDC 586/2021](https://ses.sp.bvs.br/wp-content/uploads/2021/12/U_RS-MS-ANVISA-RDC-586_171221.pdf) suspended those transmission deadlines indefinitely while keeping the bookkeeping duty. ANVISA's [notice of 05/05/2025](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2025/sngpc-retorno-da-transmissao-regular-obrigatoria) brought them back by region: Southeast from 2025-09-01, South and North from 2025-11-01, Center-West and Northeast from 2026-01-02.
- **Technical interface.** The [developer page](https://www.gov.br/anvisa/pt-br/assuntos/fiscalizacao-e-monitoramento/sngpc/desenvolvedores) lists the version 2.0 XML schemas (dated 2013-05-22) and a REST API. The API's [Swagger document](https://sngpc-api.anvisa.gov.br/swagger/index.html) has three endpoints: get a token, upload the XML file, and query a file's status. The XSD files themselves returned HTTP 403 to automated download, so their element names (for example, how a "transformação" is encoded) are not verified here.
- **Shape of a reported compounded sale.** ANVISA's [open-data dictionary for compounded sales](https://dados.anvisa.gov.br/dados/Dicionario_de_Dados_Painel_SNGPC_Venda_de_Manipulados.pdf) (version 3.00, February 2026) lists these fields:
  - the active ingredient, quantity of active per dosage unit and its unit;
  - the number of dosage units and their type, which may be only cápsula, comprimido, grama or mililitro;
  - the prescriber's council and its state;
  - the prescription form (codes 1 to 5).

  The dictionary uses testosterone as its example of a Portaria 344/1998 substance. It also says GLP-1 agonists are reported from 2026-01-02. How a hormone implant is reported is not stated there.

### Electronic prescriptions for controlled substances (SNCR, RDC 1000/2025)

ANVISA's [SNCR guide for pharmacies](https://www.gov.br/anvisa/pt-br/assuntos/medicamentos/controlados/sncr/cartilha_sncr_farmacias_vf.pdf/@@display-file/file) (1st edition, September 2026) describes the rules under RDC 1000/2025:

- From 30/09/2026, Notificações de Receita can also be issued electronically. Receitas de Controle Especial and prescriptions subject to retention already could be, but from that date they are numbered by SNCR and issued through prescribing services integrated with it.
- Between 30/09 and 29/10/2026, a transition period still admits electronic Receitas de Controle Especial and retention prescriptions without an SNCR number; this file's date falls inside it. From 30/10/2026, new ones must carry an SNCR number. One issued without a number during the transition can still be dispensed while it is valid, so the pharmacy has to check the issue date and whether a number is present.
- For a prescription with an SNCR number, the pharmacy must consult it in SNCR before dispensing and register in SNCR, after dispensing, that it was used. For one without a number, the dispensing is recorded on the document itself.
- Notificações de Receita and Receitas de Controle Especial require a qualified (ICP-Brasil) signature; prescriptions subject to retention accept an advanced or qualified one. Signatures can be checked with ITI's Validar service.
- The SNCR usage record "não elimina a escrituração da movimentação de medicamentos no SNGPC".
- Electronic prescription is not mandatory, and paper forms continue.

The guide describes screens. It mentions no API for pharmacy systems, so whether AmpolaTech can automate the SNCR steps is open.

The Conselho Federal de Medicina also runs a [free prescription service](https://sistemas.cfm.org.br/prescricaoeletronica) with templates that include "Receituário de Controle Especial". Doctors sign with ICP-Brasil, and pharmacists dispense and sign digitally, with validation at ITI. The page does not mention compounded formulas.

### Regulation pointers (not researched here)

These are listed only because they define fields the software must hold. The owner's separate regulatory research should confirm each.

- **[RDC 67/2007](https://bvsms.saude.gov.br/bvs/saudelegis/anvisa/2007/rdc0067_08_10_2007.html)** (Ministry of Health compilation, which may not include later amendments) sets several record requirements:
  - **Compounding order** (item 8.4): it records the formula with "todos os componentes (inclusive os excipientes) e concentrações", the "lote de cada matéria-prima, fornecedor e quantidade pesada", the names and signatures of whoever weighed and compounded, the pharmacist's check, and the date.
  - **Label** (item 12.1): prescriber, patient, prescription-book number, date, expiry, components with quantities, posology, and the pharmacy's identification.
  - **Shelf life** (item 15.4): based on stability, preferably tied to the treatment period.
  - **Sterile products, Annex IV** (item 9.3): each finished product needs 100% visual inspection, a label check, a sterility test and an endotoxin test, and item 9.4 says "Todas as análises realizadas devem ser registradas".
- **Annex IV is being revised.** [ANVISA's Informe 1/2025](https://www.gov.br/anvisa/pt-br/composicao/diretoria-colegiada/reunioes-da-diretoria/votos/2025/rop-10.2025/1-1.pdf/@@display-file/file) (30/06/2025) opened the process and plans an impact analysis and a public consultation.
  - Of 19 sterile-compounding pharmacies inspected in 2024, 16 were fully or partly shut down. Failures included missing non-viable particle monitoring and "falhas na comprovação da esterilidade dos produtos, antes da dispensação ao paciente".
  - The report also cites the lack of stability studies behind injectables' expiry dates.
- **Hormone implants.** On [18/10/2024 ANVISA prohibited compounded hormone implants](https://www.gov.br/anvisa/pt-br/assuntos/noticias-anvisa/2024/anvisa-faz-proibicao-e-publica-alerta-sobre-hormonios-implantaveis-manipulados) (RE 3.915/2024). Press reports say RE 4.353/2024 (November 2024) replaced it with a narrower ban, covering aesthetic, muscle-gain and sports-performance uses, plus conditions such as a CID code on the prescription and SNGPC bookkeeping. **Only secondary sources were found for RE 4.353/2024.** This decides whether pellets are a product AmpolaTech can make at all, so check the DOU text before designing around it.

## 4. What the domain needs

These capabilities recur across the products above and matter for made-to-order sterile compounding. Each lists the products that show it.

1. **Ordering limited to identified prescribers, from the pharmacy's formulary.** The order carries the prescriber's council number and is tracked by status. Shown by: LifeFile provider portal (practice formularies, real-time status), SiCompounding physician portal, RXmais (ICP-Brasil signed, QR import into FórmulaCerta), OpenMRS `Order.orderer` and `FulfillerStatus`, FHIR `MedicationRequest.requester`. RDC 67/2007 item 5.18.4 requires the prescriber's council registration on the prescription.
2. **Reading a prescription into a draft order or quote, with human review.** Shown by: Tech_IA (Fagron Tech), EasyFarma IA (TEXS), VS-Assistente (VitóriaSoft).
3. **Controlled-substance checks and reporting.** The pharmacy validates the signature and SNCR number before dispensing, registers use in SNCR, and keeps SNGPC bookkeeping per lot, including "transformação". Shown by: SNGPC generation in Pharmacie, FarmaFácil, OuroFórmulas, Medicator and Vitória Farmácia; PDMP reporting and EPCS in SiCompounding and LifeFile; the OpenLMIS event ledger as a model.
4. **Master formula with a stability-based BUD.** The formula is versioned, lists every component including excipients, and states where its BUD comes from. Shown by: OuroFórmulas "fórmulas padrão", Pestle Formulas master formulation records, Seal's dating basis linked to stability evidence, FórmulaCerta's technical-specification database, Odoo `mrp.bom` and ERPNext BOM as structural analogs, FHIR `MedicationKnowledge.storageGuideline.stabilityDuration`. A fixed shelf-life-per-product rule (Odoo `product_expiry`, ERPNext `shelf_life_in_days`) is not enough.
5. **A compounding record per preparation.** It records the component lots used, the quantities weighed, who weighed and who compounded, the pharmacist's check, and a timestamp at each step. Shown by: VitóriaSoft (lot and quantity per raw material, operator, time), DoseEdge (barcode scan per container, images), Pestle Formulas (centralized log, photo capture), Seal (electronic batch record), Compound Direct (photos on the record), and RDC 67/2007 item 8.4.
6. **Verified measurement.** The system checks each measurement against the formula, using a balance link or a gravimetric check plus a barcode check of each container. Shown by: monitored weighing in FórmulaCerta, Pharmacie, FarmaFácil and OuroFórmulas; Compound Direct ("Validate and record each measurement"); DoseEdge barcode verification.
7. **Lot genealogy from raw material to patient, for recall.** Shown by: OuroFórmulas, Pharmacie and Medicator lot traceability; Odoo's traceability report; ERPNext batch `reference_doctype`; OpenBoxes `RECALLED` lot status.
8. **Quarantine, then a QC release gate before dispensing.** The release depends on sterility and endotoxin results, and on a second person for review. Shown by: DoseEdge quarantine until pharmacist verification; Seal ("release waits for required results"); OCA `qc.inspection` states with supervisor approval; ERPNext Quality Inspection with `inspected_by` and `verified_by`; SENAITE's required number of verifications; OpenBoxes `QUARANTINED`. RDC 67/2007 Annex IV item 9.3 and ANVISA's 2024 inspection findings make this the main point for a sterile compounder.
9. **Expiry enforced where a lot is used, not only reported.** Shown by: Medicator (blocks sale), OpenMRS Dispensing (offers only batches valid through the end of therapy), PrimeRx (flags expired stock). Odoo's `mrp_product_expiry` only warns.
10. **Labels with the required fields, checked against the record.** Shown by: Compound Direct label check, DoseEdge verification label, FórmulaCerta QR-code label with lot and expiry, and RDC 67/2007 item 12.1.
11. **Cleanroom compliance records, kept apart from the preparation record.** These cover environmental monitoring, cleaning, staff competency and garbing, equipment certification, and deviations with CAPA. Shown by: Pestle Compliance, Simplifi 797 (partly verified), Seal, SiCompounding (OOS and CAPA).
12. **An audit trail and signatures that state their meaning.** Shown by: Pharmacie ("controle de qualidade com assinatura digital"), Pestle ("time-stamped and uneditable" records), Seal (who, when, and what the signature means), OpenMRS `@Audited` entities.
13. **Preparation and hand-over as separate events.** Sterility testing puts days between them. Shown by: FHIR `MedicationDispense.whenPrepared` and `whenHandedOver`, OpenMRS `datePrepared` and `dateHandedOver` (although the OpenMRS Dispensing app sets the two equal).

## Not verified

- FórmulaCerta's market leadership: vendor claim only.
- That Phusion is newer than FórmulaCerta, and the 2025 date of Fagron Tech's AI prescription reading: secondary sources only.
- RE 4.353/2024 on hormone implants: secondary sources only.
- Baxter DoseEdge features after 2014: Baxter's current product pages refused automated access.
- Simplifi 797: claims come from search excerpts of the vendor's pages, not a full reading.
- MEDDIX: the vendor's site did not resolve.
- SNGPC XSD element names: the schema files refused automated download.
- Whether pharmacy systems can reach SNCR through an API: the ANVISA guide describes screens only.
- How a hormone implant's dosage unit is reported in SNGPC: not stated in the open-data dictionary.
- OpenMRS Stock Management module's license: no LICENSE file in the repo.
