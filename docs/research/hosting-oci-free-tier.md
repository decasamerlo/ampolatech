# Hosting AmpolaTech v1 on OCI's free tier

Date: 2026-10-20

**Summary.** OCI's free tier can host the first version, but only if three conditions hold: the account is upgraded to Pay As You Go, the machine is sized to the allowance Oracle documents today, and a copy of the data lives outside Oracle. An account that stays Always Free only and holds the only copy of patient data is not a sound home. On such an account Oracle offers no SLA, no support and no continuity policy. It can end the account on 7 days' notice after a week with no activity, and it can reclaim instances it considers idle. In June 2026 it halved the Arm allowance on its Always Free page, and no announcement of the change turned up. The setup must handle three things:

- **Size for 2 OCPUs and 12 GB, not the 4 and 24 most guides cite.** Oracle's Always Free page [changed in June 2026](#ampere-a1-the-allowance-was-halved-in-june-2026). Other Oracle pages, including the contract, still say 4 and 24, so it is unclear what a paid account gets.
- **Upgrade to Pay As You Go on day one.** Inside the free limits it [costs nothing](#what-pay-as-you-go-costs). It should take the tenancy out of the clauses written for Always Free-only customers, and it gives access to support. It does not provably exempt the instance from [idle reclamation](#3-idle-reclamation), and it does not add an SLA to the free resources.
- **Keep backups that outlive the account.** Five free volume backups and 20 GB of Object Storage sit in the same tenancy and the same single region. If the account goes, they go with it. A nightly database dump copied outside OCI is the backup that counts.

How to read this file:

- Every claim links to the source it was checked against on 2026-10-20: Oracle's documentation, its free tier page and FAQ, its contract documents (Cloud Services Agreement, Service Descriptions, Hosting and Delivery Policies), its price list API, and the image publishers' own registries and docs. Where only a secondary source exists, the text says so.
- Oracle's free tier pages carry no date. Where this file says when a page changed, the dates come from Internet Archive snapshots of that Oracle page, linked as such.
- Text marked **analysis** is reasoning from the cited facts, not a sourced claim.

## 1. What is Always Free today

### The allowance

| Resource | Always Free allowance | Source |
| --- | --- | --- |
| Arm compute (`VM.Standard.A1.Flex`) | 1,500 OCPU hours and 9,000 GB hours a month, "equivalent to 2 OCPUs and 12 GB of memory", as one instance or two of 1 OCPU each. See [the June 2026 change](#ampere-a1-the-allowance-was-halved-in-june-2026). | [Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) |
| AMD compute (`VM.Standard.E2.1.Micro`) | Up to two instances, each "1/8th of an OCPU with the ability to use additional CPU resources", 1 GB of memory, "up to 50 Mbps network bandwidth via the internet" | same page |
| Block Volume | "a total of 200 GB of Block Volume storage, and five volume backups", counting boot and block volumes together, in the home region only | same page |
| Object Storage | 20 GB of Standard, Infrequent Access and Archive combined once the trial has expired, or 10 GB of each tier on a paid or trial account; 50,000 API requests a month in both cases | same page |
| Outbound data | "10 TB per month" | same page |
| Load balancing | One flexible load balancer at 10 Mbps (accounts created from December 15, 2020) and one network load balancer | same page |
| Autonomous AI Database | Two databases, each "1 Oracle CPU processor (cannot be scaled)", "20 GB storage (cannot be scaled)", 20 simultaneous sessions | same page |
| MySQL HeatWave | One standalone DB system with "50 GB of storage" and "an extra 50 GB of backup storage" | same page |
| Networking | Free Tier tenancies "can have up to 2 virtual cloud networks"; outbound TCP port 25 is blocked by default | same page |
| Email Delivery | "3000 emails for free per month" | same page |

Gaps and inconsistencies on that page:

- **Boot volume size.** The page gives the minimum boot volume as 47 GB "regardless of shape" in one paragraph, then says "the minimum boot volume size allowed for compute instances is 50 GB" in another, with 50 GB as the default. Either way, a boot volume uses up part of the 200 GB ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
- **Mixing paid and free storage.** Block Volume "automatically tries to transition an existing paid resource to an Always Free resource" when free capacity becomes available, but "Oracle does not recommend" mixing the two (same page).

### No free managed PostgreSQL

The Always Free page does not mention PostgreSQL ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)). Oracle does run a managed PostgreSQL, "Oracle Cloud Infrastructure Database with PostgreSQL", but it is absent from the contract's table of Always Free Cloud Services ([Service Descriptions, section E and the database service descriptions](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf), effective 08-October-2026). Oracle's price list API bills it at US$0.098 per OCPU hour (x86) and US$0.072 per GB-month of storage ([price list API](https://apexapps.oracle.com/pls/apex/cetools/api/v1/products/?currencyCode=USD), parts B99060 and B99062). The free databases are Oracle Database (Autonomous), MySQL and NoSQL. For AmpolaTech, PostgreSQL runs in a container on the Arm instance.

### Ampere A1: the allowance was halved in June 2026

- **The change.** Oracle's Always Free page said "3,000 OCPU hours and 18,000 GB hours per month ... equivalent to 4 OCPUs and 24 GB of memory" in the snapshot of [2026-06-05](https://web.archive.org/web/20260605112238/https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm). By the snapshot of [2026-06-12](https://web.archive.org/web/20260612144234/https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) it said 1,500 and 9,000, equivalent to 2 OCPUs and 12 GB. Between the same two snapshots, "up to four compute instances" became "one or two OCI Ampere A1 Compute instances". The Free Tier overview also tells trial users to keep "no more than 2 OCPUs and 12 GB of memory" ([Free Tier](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm)).
- **No release note found.** No Oracle release note or announcement about the change turned up. InfoQ (secondary) [reports](https://www.infoq.com/news/2026/07/oracle-cloud-free-tier-limits/) that there was none. It also reports, from users' screenshots of Oracle Support emails, that the cut does not apply to Pay As You Go tenancies. No Oracle page says that.
- **Oracle's pages disagree.** Four other Oracle sources still state the old allowance:
  - the [Arm-based Compute page](https://docs.oracle.com/en-us/iaas/Content/Compute/References/arm.htm) says "All tenancies on OCI A1 (Compute), including paid and trial accounts, get the first 3,000 OCPU hours and 18,000 GB hours per month for free";
  - the [price list](https://www.oracle.com/cloud/price-list/) says "Each paid tenancy gets the first 3,000 OCPU hours and 18,000 GB hours per month for free";
  - the price list API prices A1 at US$0 up to 3,000 OCPU hours and 18,000 GB hours ([price list API](https://apexapps.oracle.com/pls/apex/cetools/api/v1/products/?currencyCode=USD), parts B93297 and B93298);
  - the contract says that for the Free Tier of A1 "You may only use 3000 OCPU Hours per month and 18,000 Gigabyte Hours per month" ([Service Descriptions](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf), Compute section, effective 08-October-2026).
- **Unverified: what a paid tenancy gets.** Oracle's own pages do not settle it. Plan for 2 OCPUs and 12 GB. Treat 4 and 24 on a Pay As You Go account as a possible bonus, and check it in the console's service limits after upgrading (**analysis**).
- **Oracle reserves the right to cut.** The contract allows exactly this kind of change: "Oracle in its sole discretion may remove or modify an Always Free Cloud Service from the Always Free category ... at any time" ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)).

### Always Free versus the 30-day trial

- **The trial.** It is "$300 of cloud credits that are valid for up to 30 days", alongside the Always Free resources ([Free Tier](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm)). It ends when the credits run out or 30 days pass, "whichever comes first" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **Paid resources die at the end.** "Paid resources that were provisioned with your credits during your free trial are reclaimed by Oracle unless you upgrade your account" ([Free Tier](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm)). After a 30-day grace period, "your Free Trial service instances and data will be deleted", and "reclaimed resources can't be recovered—they are permanently deleted" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **Two traps at the end of the trial.**
  - With more Arm capacity than the Always Free limit, "all existing OCI Ampere A1 Compute instances are disabled and then deleted after 30 days, unless you upgrade to a paid account" ([Free Tier](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm)).
  - With more than 20 GB in Object Storage, "all of your objects will be deleted" ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
- **Special offers.** "With some special Free Trial offers, your account will not continue as an Always Free account after the trial expires": it is suspended for 30 days and then "fully deleted" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **Always Free status is fixed at creation.** Resources are "designated Always Free when they are created" and the status "is not transferable to another resource" ([FAQ](https://www.oracle.com/cloud/free/faq/)).

## 2. Regions

- **Brazil has two OCI regions, one availability domain each.** Brazil East (Sao Paulo), `sa-saopaulo-1`, and Brazil Southeast (Vinhedo), `sa-vinhedo-1`, are commercial-realm (OC1) regions with **1** availability domain each ([Regions and Availability Domains](https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm)).
- **Free Tier is offered in every commercial region, with a caveat.** Oracle says it "is currently available in all regions where commercial Oracle Cloud Infrastructure service is available". A footnote adds: "Exact regions available for Free Tier may differ during signup process" ([FAQ](https://www.oracle.com/cloud/free/faq/)). The A1 shape can be created "in any availability domain, except South Korea North (Chuncheon)", so neither Brazilian region is excluded ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
- **The home region rule.**
  - Always Free resources are free "in the home region of the tenancy". Compute "must" be created there. Block volumes outside it "incur regular block volume costs" ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
  - The overview says to "choose the home region carefully" ([Free Tier](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier.htm)).
  - It cannot be changed: "Oracle assigns your home region and you can't change it" ([Managing Regions](https://docs.oracle.com/en-us/iaas/Content/Identity/regions/managingregions.htm)). The contract says the same: "You will not be allowed to change the Home Region even if You subsequently attempt to add additional Data Center Regions" ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)).
- **One region only.** "Trial, free tier, and pay-as-you-go tenancies are limited to one subscribed region." A Pay As You Go tenancy can request an increase ([Regions and Availability Domains](https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm)). Until such a request is granted, there is no second Brazilian region for backup copies.
- **Unverified.**
  - Whether the signup form offers São Paulo or Vinhedo as a home region today. The form is a script-driven app, and checking needs an actual signup.
  - Whether Always Free Autonomous Database is supported in either region. Oracle's page sends readers to an "Always Free Cloud Services table" on oracle.com. That link now redirects to a page without such a table. This does not matter for a PostgreSQL stack.

## 3. Idle reclamation

### The policy today

Oracle's current text ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)):

> Idle Always Free compute instances may be reclaimed by Oracle. Oracle will deem virtual machine and bare metal compute instances as idle if, during a 7-day period, the following are true:
>
> - CPU utilization for the 95th percentile is less than 20%
> - Network utilization is less than 20%
> - Memory utilization is less than 20% (applies to A1 shapes only)

The page does not say:

- what "reclaimed" means (stopped or terminated);
- whether Oracle gives notice;
- what network utilization is measured against;
- whether the rule covers Always Free resources in a paid account.

### How it changed

- **November 2022.** The snapshot of [2022-11-04](https://web.archive.org/web/20221104225529/https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) scoped the rule to free accounts. It read: "Always Free accounts only. Idle resources for Always Free accounts can be reclaimed at any time, without notice. Reclaiming includes actions such as stopping or terminating." It also said a reclaimed instance "will appear as stopped in the Console" and showed how to start it again.
- **June 2023.** The snapshot of [2023-06-01](https://web.archive.org/web/20230601005006/https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) had the 7-day rule with 15% thresholds.
- **January 2024.** By the snapshot of [2024-01-01](https://web.archive.org/web/20240101163658/https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm) the thresholds were 20%, and the "Always Free accounts only" wording was gone.

### Does Pay As You Go change it?

- **Idle reclamation: unverified.** No current Oracle page says that Always Free instances in a paid account are exempt. The only Oracle wording that limited the rule to free accounts is the 2022 version above. Third-party posts quote Oracle's reclamation emails as saying that converting to Pay As You Go keeps instances from being stopped. No Oracle page states that.
- **Account termination: very likely.** The contract has a separate clause for customers who order only Always Free services: "If You only order Always Free Cloud Services, Oracle may end, upon 7 days' notice: (i) Your right to use an applicable Always Free Cloud Service(s), or (ii) the Oracle Cloud Services Account, if You have not used the Always Free Cloud Services or have no activity during the 7 day period preceding the date of the Notice" ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)). A Pay As You Go upgrade is a paid order, so the account should fall outside it. That is **analysis**: the contract defines neither "only order" nor "activity".
- **SLA and support.** "Cloud Service Continuity Policy, Cloud Service Level Agreement and Oracle Cloud Support Policy" do not apply to Always Free Cloud Services. They apply only once you "commence paying for that applicable Cloud Service" (same section). Upgrading alone does not give the free A1 instance an SLA. It does give the account access to Oracle Support: Pay As You Go customers "have access to customer support to open tickets", while "Customers using only Always Free resources are not eligible for Oracle Support" ([FAQ](https://www.oracle.com/cloud/free/faq/)).

### What Pay As You Go costs

- **Nothing inside the free limits.** "You will only be charged for services that you use that exceeds Always Free", and "No minimums and no prepayment are required for your paid account" ([FAQ](https://www.oracle.com/cloud/free/faq/)). The Always Free page repeats that "Oracle doesn't charge for Always Free resources after you upgrade" ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
- **A card hold, and no way back.** At upgrade, "your credit card is authorized for $100 USD (or its equivalent in your country)", and the authorization is "immediately reversed on the Oracle side" ([Managing Account Upgrades](https://docs.oracle.com/en-us/iaas/Content/Billing/Tasks/changingpaymentmethod.htm)). "There is no option to downgrade your account" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **Over-limit rates.** From Oracle's [price list API](https://apexapps.oracle.com/pls/apex/cetools/api/v1/products/?currencyCode=USD), in USD:
  - A1: US$0.01 per OCPU hour and US$0.0015 per GB hour past the free tier (parts B93297, B93298).
  - Block volume: US$0.0255 per GB-month plus US$0.0017 per performance unit per GB-month (B91961, B91962).
  - Object Storage: US$0.0255 per GB-month past 10 GB (B91628).
  - Outbound data from South America: US$0.025 per GB past 10,240 GB (B93455).
- **Analysis.** If a paid tenancy gets only 2 OCPUs and 12 GB free, a third and fourth OCPU with 12 more GB would cost about US$28 a month (2 × 744 h × 0.01 + 12 × 744 h × 0.0015).

### Will this workload look idle?

**Analysis.** It likely will on CPU and network: a Java backend and PostgreSQL serving a handful of staff will sit far below 20% of 2 OCPUs. Memory is the only condition likely to stay above 20% (2.4 GB of 12 GB), because a JVM heap plus PostgreSQL's shared buffers should exceed it.

Oracle does not say how it measures memory for this rule. The metric an owner can watch is `MemoryUtilization` from the instance's monitoring plugin: "Space currently in use. Measured by pages. Expressed as a percentage of used pages" ([Compute Instance Metrics](https://docs.oracle.com/en-us/iaas/Content/Compute/References/computemetrics.htm)). Upgrading is a better defence than tuning memory to stay above a threshold.

## 4. Capacity

Oracle's own statements:

- **What the error means.** An "out of host capacity" error "indicates a temporary lack of Always Free shapes in your home region". Oracle "is working to provide more capacity, though it might take several days before additional capacity is available in your home region" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **What Oracle recommends.**
  - "If your home region has multiple availability domains, try creating the instance in a different availability domain. If that doesn't work, wait a while, and then try to launch the instance again" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
  - The Always Free page adds: "You can also choose to upgrade your account to Pay as You Go or another Paid account type, which gives you access to more types of Compute resources" ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
  - Elsewhere the FAQ notes that "Pay As You Go accounts are subject to different capacity limits than Always Free accounts" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **General troubleshooting.** Oracle's page for the error: "Capacity is dynamic and depends on the total pool size". Its advice: change the shape, the fault domain or the availability domain, and do not specify a fault domain so that "the best possible Fault Domain is assigned" ([Resolving Out of Host Capacity error](https://docs.oracle.com/en-us/iaas/Content/Compute/Tasks/troubleshooting-out-of-host-capacity.htm)).
- **Fine print.** "Availability to Free Tier is subject to capacity limits" ([FAQ](https://www.oracle.com/cloud/free/faq/), footnote).
- **Checking before launch.** The CLI command `oci compute compute-capacity-report create` "Generates a report of the host capacity within an availability domain that is available for you to create compute instances" ([OCI CLI reference](https://docs.oracle.com/en-us/iaas/tools/oci-cli/latest/oci_cli_docs/cmdref/compute/compute-capacity-report/create.html)).

What this means in Brazil:

- **No other availability domain to try.** Both regions have one, so Oracle's first suggestion does not apply. Fault domains, waiting, and upgrading are what remain (**analysis**, from the [region table](https://docs.oracle.com/en-us/iaas/Content/General/Concepts/regions.htm)).
- **Unverified: capacity right now.** Oracle publishes no per-region capacity figures. Whether A1 capacity is available in São Paulo or Vinhedo today can only be learned by trying to launch, or by running the capacity report from inside a tenancy.
- **Analysis: keep the instance running.** An instance that is terminated, whether by the owner, by reclamation, or by a resize gone wrong, may not be recreatable while capacity is short. The fallback inside the free tier is the AMD micro shape with 1 GB of memory, which cannot run this stack.

## 5. Running this stack on Arm64

### Images

- **Java 25.** `eclipse-temurin:25-jre` and `eclipse-temurin:25-jdk` are multi-architecture images that include `linux/arm64/v8`. Both were last pushed on 2026-10-17 (Docker Hub tag data for [`25-jre`](https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/25-jre) and [`25-jdk`](https://hub.docker.com/v2/repositories/library/eclipse-temurin/tags/25-jdk)). `25-jre-alpine` also has arm64.
  - The image is a Docker Official Image "Maintained by [Adoptium](https://github.com/adoptium/containers)" ([Docker Hub](https://hub.docker.com/_/eclipse-temurin)).
  - Adoptium's API lists 25 as the most recent LTS, and its latest 25 JRE for Linux aarch64 as `jdk-25.0.4.1+1` ([available releases](https://api.adoptium.net/v3/info/available_releases), [latest 25 aarch64 JRE](https://api.adoptium.net/v3/assets/latest/25/hotspot?os=linux&architecture=aarch64&image_type=jre)).
- **PostgreSQL.** `postgres:18` (18.6) includes `linux/arm64/v8`, last pushed on 2026-10-07 ([tag data](https://hub.docker.com/v2/repositories/library/postgres/tags/18)).
  - The PostgreSQL project does not publish this image. It is a Docker Official Image "Maintained by [the PostgreSQL Docker Community](https://github.com/docker-library/postgres)" ([Docker Hub](https://hub.docker.com/_/postgres)).
- **Host OS.** The Always Free-eligible A1 images are Oracle Linux, Oracle Linux Cloud Developer and Ubuntu ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
  - Docker Engine supports Ubuntu on arm64 (22.04, 24.04, 26.04 and 26.10) ([Install on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)).
  - Oracle Linux is not in Docker's list of supported platforms; for other distributions, Docker "doesn't test or verify installation" ([Install Docker Engine](https://docs.docker.com/engine/install/)).
  - **Analysis:** Ubuntu is the safer host.
- **Building for Arm.** CI on x86 has to build `linux/arm64` images too. Docker's multi-platform builds support QEMU emulation, which "can be much slower than native builds", as well as native nodes and cross-compilation ([Multi-platform builds](https://docs.docker.com/build/building/multi-platform/)). Publishing both `amd64` and `arm64` keeps a move to an x86 VPS a pull, not a rebuild (**analysis**).

### Firewall gotchas on OCI's Ubuntu image

- **Oracle:** "Do not use Uncomplicated Firewall (UFW) to edit firewall rules on an Ubuntu image. Using UFW to edit rules might cause an instance not to boot" ([Best Practices for Compute](https://docs.oracle.com/en-us/iaas/Content/Compute/References/bestpracticescompute.htm)).
- **Docker:** "when you expose container ports using Docker, these ports bypass your firewall rules" when ufw or firewalld manage them ([Install on Ubuntu](https://docs.docker.com/engine/install/ubuntu/)).
- **Analysis.** Control exposure in the VCN's security lists or network security groups. Publish only the reverse proxy's ports from Compose, and keep PostgreSQL on an internal Compose network with no published port.

### Backups for PostgreSQL in Docker

What OCI offers inside the free allowance:

- **Block volume backups.**
  - Backups of boot and block volumes are "encrypted and regionally stored in Object Storage". They can be incremental or full, and "no functional difference exists" between the two for recovery ([Block Volume Backups](https://docs.oracle.com/en-us/iaas/Content/Block/Concepts/blockvolumebackups.htm)).
  - Policy-based backups run at most once a day, and "Manual backups don't expire" (same page).
  - The Always Free allowance is five backups at any time; past that, "the operation will fail with an error until you delete existing Always Free volume backups" ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
- **Oracle's predefined policies hold more than five.** Bronze keeps twelve monthly backups plus a yearly one, and Silver and Gold keep more ([Backup Policies](https://docs.oracle.com/en-us/iaas/Content/Block/Tasks/schedulingvolumebackups.htm)). To stay at five, use a user-defined policy with short retention.
- **Consistency.**
  - Volume backups capture "only the data on the disk", and app-consistent backups need the application quiesced first ([Block Volume Backups](https://docs.oracle.com/en-us/iaas/Content/Block/Concepts/blockvolumebackups.htm)).
  - PostgreSQL accepts a "frozen snapshot" of a running server: on restore, the server "will think the previous server instance crashed and will replay the WAL". That holds only if the WAL is in the snapshot and all volumes were snapshotted at the same instant ([File System Level Backup](https://www.postgresql.org/docs/18/backup-file.html)).
  - **Analysis:** keep the PostgreSQL data directory and WAL on one volume, or back up the volumes as a volume group, which gives "cross-volume, crash-consistent backups" ([Block Volume Backups](https://docs.oracle.com/en-us/iaas/Content/Block/Concepts/blockvolumebackups.htm)).
- **Cross-region copy.** Block Volume can copy backups to another region (same page), but a free or Pay As You Go tenancy is limited to one subscribed region unless an increase is granted (see [Regions](#2-regions)).
- **Object Storage.**
  - 20 GB and 50,000 requests a month (see [the allowance](#the-allowance)).
  - It speaks the Amazon S3 Compatibility API with a "Customer Secret Key", so S3-capable backup tools (`pg_dump` piped to an S3 client, pgBackRest, WAL-G) can write to it ([S3 Compatibility API](https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/s3compatibleapi.htm)).
  - Retention rules block modification and deletion for a set duration. "Locking a retention rule is an irreversible operation", with a "mandatory 14-day delay" before the lock takes effect ([Retention Rules](https://docs.oracle.com/en-us/iaas/Content/Object/Tasks/usingretentionrules.htm)).
- **Backups are the customer's job.** "For Oracle Cloud Services which enable You to configure backups in accordance with Your own policies, You are responsible for performing backups and restores of Your Content" ([Hosting and Delivery Policies](https://www.oracle.com/contracts/docs/ocloud_hosting_delivery_policies_3089853.pdf), section 2.2, September 2026, v3.13).

**Analysis.**

- **What the free storage holds.** A compressed `pg_dump` of a single pharmacy's data should stay well inside 20 GB for a long time, and one upload a night uses a few dozen requests a month.
- **Request budget for WAL archiving.** Continuous WAL archiving pushes at least one object per WAL segment. Check the request count against the 50,000-a-month ceiling before turning it on.
- **Everything above dies with the account.** Volume backups, Object Storage and retention rules all live in the same tenancy and region as the database. Retention rules protect against a stolen key, not against Oracle ending the account.

## 6. Terms that matter

- **Production use is not banned, but Oracle frames the free tier as being for learning and testing.**
  - The FAQ says the free tier "enables you to learn, explore, build and test for free". It adds that "as you move from development and testing to production scenarios, you may need to use resources outside of Always Free limits" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
  - The Always Free page says the resources let you "run small-scale applications or perform proof-of-concept testing" ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)).
  - No clause that bans production use of Always Free services turned up. Where Oracle does mean it, it says so: Database Developer entitlements are "for non-production use only", and a trial's retrieval period "may not be used for any production activities" ([Service Descriptions, section F](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf); [Hosting and Delivery Policies, section 6.1](https://www.oracle.com/contracts/docs/ocloud_hosting_delivery_policies_3089853.pdf)).
- **No SLA, no continuity, no support.** These do not apply to Always Free Cloud Services ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)). "Oracle Cloud Free Tier does not include SLAs" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
- **Ways Oracle can end the account or a service.**
  - On 7 days' notice for a customer who orders only Always Free services and had "no activity" in the 7 days before the notice ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)).
  - "Accounts left idle for 30 days or more may be deemed abandoned and become eligible for suspension or termination" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
  - For "unusual activity" that violates section 9.3 of the Cloud Services Agreement ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)). That section covers threats to the service, illegal use, Acceptable Use Policy violations, and false account or payment information ([Cloud Services Agreement, 9.3](https://www.oracle.com/contracts/docs/cloud_csa_online_v062223_us_eng.pdf)).
  - "One Oracle Cloud Free Trial or Always Free account is permitted per person", and "creating or attempting to create multiple free accounts is prohibited" ([FAQ](https://www.oracle.com/cloud/free/faq/)).
  - Oracle "may remove or modify an Always Free Cloud Service ... at any time" ([Service Descriptions, section E](https://www.oracle.com/contracts/docs/paas_iaas_universal_credits_3940775.pdf)).
- **Data deletion.**
  - Generally: "At the end of the Services Period, we will make Your Content ... available for retrieval by You during a retrieval period ... Following the retrieval period ... we will delete any of Your Content that remains in the Services" ([Cloud Services Agreement, 9.5](https://www.oracle.com/contracts/docs/cloud_csa_online_v062223_us_eng.pdf)).
  - The retrieval period is 60 days, including after "Your termination of Cloud Services that You consume in a Pay as You Go model", and 30 days "following end of the trial or pilot" ([Hosting and Delivery Policies, section 6.1](https://www.oracle.com/contracts/docs/ocloud_hosting_delivery_policies_3089853.pdf)).
  - **Unverified.** No Oracle text found states a retrieval period for an Always Free-only account that Oracle ends for inactivity.
- **Regulated health data.**
  - The Cloud Services Agreement says Your Content "may not include any data that imposes specific data security, data protection, or regulatory obligations on Oracle in addition to or different from" the Data Processing Agreement and the service terms, naming "certain regulated health or payment card information" as an example. It adds: "You are responsible for complying with Your specific regulatory, legal or data security obligations" ([Cloud Services Agreement, 5.4](https://www.oracle.com/contracts/docs/cloud_csa_online_v062223_us_eng.pdf)).
  - Oracle's Data Transfer Annex names Brazil's LGPD. Where Oracle's global access under the Data Processing Agreement moves personal data out of Brazil, it applies the Brazilian Standard Contractual Clauses ([Data Transfer Annex, Article 7](https://www.oracle.com/contracts/docs/data-transfer-annex-v060225.pdf)).
  - Under the LGPD, health data is sensitive personal data (art. 5, II), and controllers and operators must protect data against "destruição, perda" (art. 46) ([Lei 13.709/2018](https://www.planalto.gov.br/ccivil_03/_ato2015-2018/2018/lei/l13709.htm)).
- **Which agreement applies.** The FAQ links a US-English Cloud Services Agreement with Oracle America, Inc. ([FAQ](https://www.oracle.com/cloud/free/faq/); [CSA](https://www.oracle.com/contracts/docs/cloud_csa_online_v062223_us_eng.pdf)). That agreement grants use "solely for Your internal business operations" (1.1).
  - **Unverified:** whether a signup from Brazil is shown a local version.
  - **Analysis:** who owns the account (the pharmacy, or whoever operates AmpolaTech for it) decides whose "internal business operations" those are. Settle that before signup, because one person gets one free account.

## 7. Comparison: cheapest paid VPS in São Paulo

Prices checked on 2026-10-20 at each provider's own source. AmpolaTech needs about 4 GB of memory for a JVM and PostgreSQL together (**analysis**), so that row is the one to compare. OCI's free A1 gives 12 GB.

| Provider | Brazilian region | Smallest plan | 2 GB | 4 GB | Source |
| --- | --- | --- | --- | --- | --- |
| AWS Lightsail | São Paulo (`sa-east-1`) | US$5/month (0.5 GB) | US$12 | US$24 (2 vCPU, 80 GB SSD) | [regions](https://docs.aws.amazon.com/lightsail/latest/userguide/understanding-regions-and-availability-zones-in-amazon-lightsail.html), [pricing](https://aws.amazon.com/lightsail/pricing/), [price list API, sa-east-1](https://pricing.us-east-1.amazonaws.com/offers/v1.0/aws/AmazonLightsail/current/sa-east-1/index.json) |
| Vultr | São Paulo (`sao`) | US$7.50/month (1 vCPU, 1 GB) | US$15 (1 vCPU) | US$30 (2 vCPU, 80 GB) | [regions API](https://api.vultr.com/v2/regions), [plans API](https://api.vultr.com/v2/plans?per_page=500) |
| Magalu Cloud | São Paulo (`br-se1`) | R$34.99/month (1 vCPU, 1 GB, 10 GB) | R$44.99 (1 vCPU, 10 GB) | R$82.99 (2 vCPU, 10 GB); R$102.99 with 40 GB | [prices](https://magalu.cloud/precos/), [SKU API, br-se1](https://api.magalu.cloud/sku/v0/skus?region=br-se1), [regions](https://docs.magalu.cloud/docs/storage/block-storage/additional-explanations/arquitetura/) |
| DigitalOcean | none | — | — | — | [regional availability](https://docs.digitalocean.com/platform/regional-availability/) |

Notes on the table:

- **Lightsail.** Prices are the Linux bundles with a public IPv4 address. São Paulo bundles cost the same as the US ones but "include half the data transfer allowances" ([pricing](https://aws.amazon.com/lightsail/pricing/)).
- **Vultr.** The São Paulo prices come from the `location_cost` field of Vultr's public plans API, Regular Cloud Compute (`vc2`). They are 1.5 times the base price. Vultr's pricing page refused automated requests.
- **Magalu Cloud.** Prices are the Balanced Value instances, in reais, from the SKU API that the calculator on Magalu's price page calls. Magalu also sells managed PostgreSQL from R$94.22 a month (1 vCPU, 4 GB, 10 GB, single instance) in the same API.
- **DigitalOcean.** Its 13 regions include none in South America (page last verified by DigitalOcean on 9 Oct 2026).

## What the setup must handle (analysis)

This section is reasoning from the findings above, not a sourced claim.

1. **Upgrade to Pay As You Go before relying on the account.** The cost inside the limits is zero. The gains: it avoids the trial-end reclamation, should take the account out of the 7-day inactivity clause, gives access to support, and may keep the 4-OCPU A1 allowance. To stop a bill appearing by accident, set compartment quotas, which Oracle suggests for exactly this ([Always Free Resources](https://docs.oracle.com/en-us/iaas/Content/FreeTier/freetier_topic-Always_Free_Resources.htm)), so that usage stays at or under:
   - 2 OCPUs and 12 GB of A1;
   - 200 GB of block storage;
   - five volume backups.
2. **During the trial, create nothing beyond Always Free.** Paid resources made with the credits are reclaimed. A1 over the limit gets every A1 instance deleted, and over 20 GB of objects gets every object deleted, unless the account is upgraded first.
3. **Size the stack for one A1 instance with 2 OCPUs and 12 GB.** Two plausible storage layouts:
   - a 50 GB boot volume plus a 150 GB block volume holding the PostgreSQL data and WAL together;
   - one larger boot volume.

   Set the JVM heap and PostgreSQL's memory explicitly. Watch `MemoryUtilization` and `CpuUtilization`, and alarm on the instance leaving the running state.
4. **Choose the home region once.** São Paulo and Vinhedo each have one availability domain, and the choice is permanent and single-region. Try to create the A1 instance right after signup. If it hits "out of host capacity", retry over several days or upgrade. There is no other availability domain to fall back to.
5. **Back up in three layers and test the restore.**
   - A nightly `pg_dump` (or pgBackRest) to OCI Object Storage through the S3 API, with a retention rule.
   - The same dump copied outside OCI. A Brazilian location keeps the LGPD picture simple: Magalu Cloud's `br-se1` object storage, or storage the pharmacy controls.
   - One volume backup through a user-defined policy that stays within five.

   The off-OCI copy is the one that survives reclamation, account termination, or a free-tier change.
6. **Make the move cheap.**
   - Keep the Compose file, provisioning script and secrets handling in the repo.
   - Publish `amd64` and `arm64` images.

   Moving to Lightsail at US$24, Vultr at US$30 or Magalu at R$83 to R$103 a month for 4 GB is then a restore, not a rebuild.
7. **Lock down the network the OCI way.**
   - Open only 443 (and 80 for certificate issuance) in the security list.
   - Restrict SSH, or use the free Bastion service.
   - Leave UFW off on Ubuntu, and publish only the reverse proxy's ports, since Docker bypasses host firewall tools.
8. **Send email through Email Delivery or SMTP submission.** Outbound port 25 is blocked. Email Delivery includes 3,000 emails a month.

The open questions, which only a real signup or Oracle in writing can answer:

- whether a Pay As You Go tenancy gets 2 or 4 free OCPUs;
- whether Pay As You Go exempts the instance from idle reclamation;
- whether São Paulo or Vinhedo has A1 capacity on the day of signup.
