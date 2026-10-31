# Lab hardware from the browser

Date: 2026-10-20

**Summary.**

- **What the browser does alone.** On the lab's desktop Chrome or Edge, a hosted page can handle scanners and balances without installing anything.
  - A USB scanner in keyboard-wedge mode is just a keyboard. A prefix the scanner sends, plus its AIM symbology identifier, lets the page tell a scan from typing.
  - [Web Serial](#web-serial-on-the-lab-pcs) (desktop Chrome 89 and later) reads a balance on RS-232 or a USB virtual COM port. A Chrome enterprise policy can pre-grant the port, so nobody has to pick it again after a replug.
- **What needs a bridge: label printing.** A page cannot open a raw TCP socket to a printer. WebUSB to a USB printer fails on Windows because the printer driver holds the device. That leaves four routes:
  - the [print dialog](#route-1-the-browsers-print-dialog): it works, but either shows a dialog for every label, or, with Chrome's `SilentPrintingEnabled` policy, sends *every* print job to the default printer;
  - a [printer that connects out](#route-3-the-printer-connects-out-zebra-weblink) to our backend: Zebra Weblink, on full Link-OS models only;
  - a [bought bridge](#existing-products): QZ Tray at US$749 a year for silent printing, or PrintNode from US$9 a month;
  - [our own agent](#the-cost-of-writing-our-own-agent).
- **What this means for tablets.**
  - iPadOS has none of Web Serial, WebUSB, WebHID or Web Bluetooth, and every iPad browser is WebKit. An iPad can scan, with a Bluetooth scanner in keyboard mode or with the camera through a WebAssembly decoder, and can fill forms. It cannot talk to a balance or a printer itself.
  - Android Chrome has WebUSB, Web Bluetooth and Web Serial. Web Serial over Bluetooth arrived in 138; wired Web Serial arrived in 148 but only returns ports on Android builds that expose them.
  - A tablet can show a weighing or trigger a label only if a lab PC or a printer reaches the backend and the tablet talks to the backend. The [options at the end](#options-for-ampolatech-analysis) are built around that.

How to read this file:

- Every claim links to the source it was checked against on 2026-10-20: a spec, MDN's compatibility data, Chrome Platform Status, Chrome release notes, Chromium source, or the vendor's own manual or product page. Where only a secondary source exists, the text says so.
- Chrome versions are milestones. Today's stable is 156, and 157 ships on 2026-11-03 ([Chromium Dash](https://chromiumdash.appspot.com/fetch_milestone_schedule)). Edge is built on Chromium and tracks the same milestones; where Edge differs, the text says so.
- Chromium source links point at `main` as of this date, so line content can drift.
- Text marked **analysis** is reasoning from the cited facts, not a sourced claim.

## 1. Barcode scanners

### Keyboard-wedge mode

Most scanners ship as a USB keyboard. The Zebra DS2208 defaults to "USB Keyboard HID", and out of the box it is not ready for GS1 codes ([DS2208 Product Reference Guide](https://www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/guide/product/ds2208-prg-en.pdf), MN-002874-14EN, Oct 2025):

- **Keyboard layout.** The default is "US English (North American)". The guide offers "Portuguese (Brazilian ABNT)" and "Portuguese (Brazilian ABNT2)". For garbled output its fix is to "program the system for the correct keyboard type, and turn off the CAPS LOCK key". A "Keypad Emulation" mode sends every character as an Alt+numpad sequence for layouts that aren't listed (p. 7-12).
- **GS1 symbologies are off.** The defaults table lists "Data Matrix … Enable", but "GS1 Data Matrix … Disable" and "GS1 QR … Disable" (p. 12-90, 12-94).
- **Code ID.** "Transmit Code ID Character" defaults to None (p. 5-30). Set to AIM, it sends "]d2" before a GS1 DataMatrix and "]Q3" before a GS1 QR (Appendix E).
- **Prefix and suffix.** One prefix and two suffixes are configurable, from a table that includes F1–F12 (Appendix I). Nothing is added until "Scan Data Transmission Format" is changed from "Data As Is" (p. 5-31).
- **The GS separator.** It is sent as a control key: "1029 %C CTRL ] GS" (Table I-1). The guide says "ASCII values under 32 are normally sent as a control-key sequence" (p. 7-14). An "FN1 substitution" option replaces the separator with another key, but the guide describes it only for GS1-128 (p. 5-34, p. 7-13).

A Brazilian brand behaves the same way. Elgin's EL250 2D scanner manual lists "Teclado HID USB / USB Virtual COM" interfaces, a "Português do Brasil (ABNT2)" layout, configurable prefix and suffix, and GS1 DataMatrix ([EL250 manual](https://www.dicomp.com.br/downloads/materiais_apoio/datasheet/33440-2.pdf), Elgin's own manual on a distributor's site; elgin.com.br shows no specs without JavaScript). Honeywell's 1470g has "Function Code Transmit", enabled by default, which converts function codes "to a key code" in keyboard-wedge mode ([1470g Quick Start Guide](https://prod-edam.honeywell.com/content/dam/honeywell-edam/sps/ppr/en-us/public/products/barcode-scanners/general-purpose-handheld/1470g/documents/sps-ppr-vg1470-en-qs.pdf)). Honeywell's full user guides redirect to "Access Restricted", so its Brazil layout code and USB modes are unverified.

### Telling a scan from typing

A scan arrives as ordinary trusted key events. Nothing on a `KeyboardEvent` identifies the device ([UI Events](https://www.w3.org/TR/uievents/)). The page has three signals:

- **A sentinel prefix and suffix**, set in the scanner (vendor manuals above). As a bare key, Chrome on Windows uses F1, F3, F5, F6, F7, F10, F11 and F12 ([Chrome keyboard shortcuts](https://support.google.com/chrome/answer/157179)), and Edge also uses F4 and F9 ([Edge keyboard shortcuts](https://support.microsoft.com/en-us/edge/keyboard-shortcuts-in-microsoft-edge)). F2 and F8 are free in both, so either can serve as a prefix without fighting the browser.
- **The AIM symbology identifier.** GS1's spec says scanners "SHOULD have symbology identifiers enabled" for symbols with FNC1 in first position ([GS1 General Specifications](https://ref.gs1.org/standards/genspecs/) §5.4.7.4). The identifier also tells a GS1 DataMatrix (`]d2`) from a plain one.
- **Inter-key timing.** No primary source gives a threshold. The scanner's own keystroke delay is configurable (DS2208: none, 20 ms, 40 ms; p. 7-7), so a timing heuristic is a guess unless the scanners are configured to match.

Layout matters for the parser. `KeyboardEvent.code` is "based only on the key's physical location on the keyboard" and does not vary with locale ([UI Events code values](https://www.w3.org/TR/uievents-code/)). `key` is what the OS layout makes of it. If the scanner's layout setting and the PC's layout disagree, `key` is wrong. Chrome's `navigator.keyboard.getLayoutMap()` ([Keyboard Map draft](https://wicg.github.io/keyboard-map/); Chrome 69+, not Safari or Firefox per [MDN data](https://github.com/mdn/browser-compat-data/blob/main/api/Keyboard.json)) could detect a mismatch.

The GS separator is the trap. In keyboard mode it arrives as Ctrl+]. UI Events dispatches `beforeinput`/`input` only for keys "associated with a character", so a plain `<input>` most likely drops it and only a `keydown` handler sees it (analysis; needs a bench test). Without the separator, a lot number followed by another variable-length field can't be split.

### Other USB modes: virtual COM and HID POS

Two scanner modes avoid the keyboard path entirely.

- **USB virtual COM (CDC).** The DS2208 has "USB CDC Host" mode, for which Zebra says to "install the appropriate USB CDC Driver on the host" (p. 7-5). The EL250 has "USB Virtual COM". The page then reads raw bytes with Web Serial: the GS separator arrives as byte 29, the AIM ID is intact, and keyboard layout and input focus no longer matter. The cost is one more serial permission per scanner (see [Web Serial](#web-serial-on-the-lab-pcs)).
- **HID POS.** The DS2208 offers "USB HID POS (for Windows 10 devices only)" (p. 7-6). The USB-IF Barcode Scanner page (0x8C) defines a "Scanned Data Report" with "Symbology Identifier" usages and "Decoded Data" sent in chunks for long 2D codes ([HID Usage Tables 1.7](https://usb.org/sites/default/files/hut1_7.pdf) §32). WebHID blocks keyboards and mice ([Chrome WebHID docs](https://developer.chrome.com/docs/capabilities/hid); [WebHID blocklist](https://github.com/WICG/webhid/blob/main/blocklist.txt)), so it can't read a keyboard-wedge scanner. Page 0x8C is in neither Chromium's protected list ([`hid_report_utils.cc`](https://github.com/chromium/chromium/blob/main/services/device/public/cpp/hid/hid_report_utils.cc)) nor the blocklist, so a scanner in HID POS mode should be readable on desktop. That is inferred from the source, not tested.

### GS1 DataMatrix and GS1 QR

From the [GS1 General Specifications](https://ref.gs1.org/standards/genspecs/), Release 26.0:

- **Element strings.** (01) GTIN is fixed at 14 digits. (17) expiry is YYMMDD. (10) batch/lot and (21) serial are up to 20 characters and variable length.
- **Separators.** A variable-length element "SHALL be terminated by a separator character, unless it is the last element string" (§7.8.6.2). "The value of the decoded separator character transmitted in the decoded data string is always control character `<GS>` (ASCII value 29)", and "some receiving systems may convert/interpret the control character `<GS>` as something other than ASCII value 29" (§7.8.4).
- **Expiry day 00.** It means the last day of the month. "For regulated healthcare products … starting 1 January 2025 … the day of the month SHALL NOT be expressed as two zeroes" (§3.4.7).
- **Symbology identifiers.** `]d2` marks a GS1 DataMatrix "that have a leading FNC1 character" (§5.6.3.3), and `]Q3` a GS1 QR (Table 5-37).

Brazil's per-pack medicine serialization does not apply to AmpolaTech's inputs:

- **What RDC 157/2017 set up.** It defined a DataMatrix carrying GTIN, ANVISA registration, serial, expiry and lot ([RDC 157/2017](https://bvsms.saude.gov.br/bvs/saudelegis/anvisa/2017/rdc0157_11_05_2017.pdf), arts. 4 and 6).
- **That rule is revoked.** AnvisaLegis marks RDC 157 "Revogada pela … RDC nº 886, de 10/07/2024" ([AnvisaLegis](https://anvisalegis.datalegis.net/action/ActionDatalegis.php?acao=detalharAto&tipo=RDC&numeroAto=00000157&seqAto=000&valorAno=2017&orgao=RDC%2FDC%2FANVISA%2FMS&nomeTitulo=codigos&cod_modulo=310&cod_menu=9882)).
- **The law dropped the serial.** [Lei 14.338/2022](https://www.planalto.gov.br/ccivil_03/_ato2019-2022/2022/lei/l14338.htm) removed the unique serial number from [Lei 11.903/2009](https://www.planalto.gov.br/ccivil_03/_ato2007-2010/2009/lei/l11903.htm), which covers "as embalagens de todos os medicamentos registrados".
- **What it means here (analysis).** Raw materials and compounded preparations aren't registered medicines. A GS1 code on an incoming input is the supplier's choice, so the parser must also accept plain codes and labels with no code at all.

### Camera scanning on tablets

- **Native `BarcodeDetector`.**
  - **Where it runs.** Android Chrome 83 and later. Desktop Chrome on "ChromeOS and macOS only". Edge on macOS only. Safari 17 and later, iPadOS included, only behind the "Shape Detection API" preference ([MDN compat data](https://github.com/mdn/browser-compat-data/blob/main/api/BarcodeDetector.json)). Chromium enables it as stable only on Android, ChromeOS and Mac ([`runtime_enabled_features.json5`](https://github.com/chromium/chromium/blob/main/third_party/blink/renderer/platform/runtime_enabled_features.json5)).
  - **Android needs Google Play Services.** The detector returns nothing if "Google Play Services not available" ([`BarcodeDetectionProviderImpl.java`](https://github.com/chromium/chromium/blob/main/services/shape_detection/android/java/src/org/chromium/shape_detection/BarcodeDetectionProviderImpl.java)).
  - **It can't see GS1.** A `DetectedBarcode` has only `rawValue`, `format`, a bounding box and corner points. `data_matrix` has no GS1 variant, and the spec says nothing about FNC1 ([Shape Detection API](https://wicg.github.io/shape-detection-api/)).
- **WebAssembly ZXing.** [zxing-wasm](https://github.com/Sec-ant/zxing-wasm) (MIT, v3.1.6, 2026-10-19) wraps [zxing-cpp](https://github.com/zxing-cpp/zxing-cpp) (Apache-2.0, v3.1.1).
  - Its result carries `symbologyIdentifier`, a `contentType` that can be "GS1", and an "Escaped" text mode that writes ASCII 29 as `<GS>`.
  - The reader is about 1 MiB of wasm. By default it is fetched from jsDelivr; `locateFile` lets us self-host it.
  - [barcode-detector](https://github.com/Sec-ant/barcode-detector) (MIT, v3.2.3, 2026-10-19) is a `BarcodeDetector` ponyfill on the same engine. Its polyfill form steps aside when a native detector exists; the ponyfill form always uses ZXing.
- **Older libraries.** [zxing-js/library](https://github.com/zxing-js/library) (Apache-2.0) says "Project in Maintenance Mode Only". [html5-qrcode](https://github.com/mebjas/html5-qrcode) says "maintenance mode … Pull requests also won't be merged", and its last release was 2023-04-15.
- **Camera access.** `getUserMedia` needs a secure context and always asks for permission ([MDN](https://developer.mozilla.org/en-US/docs/Web/API/MediaDevices/getUserMedia)). Safari has had it since 11, with iOS following ([MDN compat data](https://github.com/mdn/browser-compat-data/blob/main/api/MediaDevices.json)).
  - In a web app saved to the iPad home screen, camera access was fixed in iOS 13.4 ([WebKit bug 185448](https://bugs.webkit.org/show_bug.cgi?id=185448)). Two open bugs report the camera not starting in that mode on some devices ([273938](https://bugs.webkit.org/show_bug.cgi?id=273938), [282327](https://bugs.webkit.org/show_bug.cgi?id=282327)), so a plain Safari tab is the safer target.

**Analysis.** Using the ZXing ponyfill on every platform gives one code path. It reads GS1 the same way on the iPad, Android and the Windows PCs, where native `BarcodeDetector` doesn't exist.

## 2. Label printers

### What printers in Brazil speak

Every vendor checked speaks ZPL or emulates it. Argox's ZPL link is only implied: its PPLZ emulation lists Zebra's GRF graphics format.

| Vendor and model | Languages | Interfaces | Source |
| --- | --- | --- | --- |
| Zebra ZD220 | ZPL II, EPL2; "Link-OS Basic" | USB | [spec sheet](https://www.zebra.com/content/dam/zebra_dam/en/tech-specs/zd220-tech-specs-en-us.pdf) |
| Zebra ZD230 | ZPL II, EPL2; Link-OS Basic | USB; factory Ethernet or Wi-Fi/Bluetooth | [spec sheet](https://www.zebra.com/content/dam/zebra_dam/en/tech-specs/zd230-tech-specs-en-us.pdf) |
| Zebra ZD421 | ZPL II, EPL2; full Link-OS | USB; field-installable Ethernet, serial, Wi-Fi | [spec sheet](https://www.zebra.com/content/dam/zebra_dam/en/tech-specs/zd421-tech-specs-en-us.pdf) |
| Zebra ZD621 | ZPL II, EPL2; full Link-OS | Ethernet, serial, USB | [spec sheet](https://www.zebra.com/content/dam/zebra_dam/en/tech-specs/zd621-tech-specs-en-us.pdf) |
| Zebra ZT231 | ZPL II, EPL2 (203 dpi only) | USB, serial, Ethernet; Wi-Fi option | [spec sheet](https://www.zebra.com/content/dam/zebra_dam/en/tech-specs/zt231-tech-specs-en-us.pdf) |
| Argox OS-214EX Pro | "Emulation PPLA, PPLB, PPLZ" | Ethernet, USB, RS-232 | [product page](https://www.argox.com/products-detail/os-214ex_pro-os-200_pro/) |
| Argox CP-2140EX Pro, iX4-250 Pro | PPLA, PPLB, PPLZ | Ethernet, USB, RS-232 | [CP-2140EX](https://www.argox.com/products-detail/cp-2140ex_pro/), [iX4-250](https://www.argox.com/products-detail/ix4-250_pro/) |
| Elgin L42 Pro Full (made in Brazil) | "EPL, ZPL, PPLA e PPLB" | USB, Ethernet, serial | [product page](https://www.elgin.com.br/impressora-termica-eti-l42-pro-full-102-mms-compativel-com-epl-zpl-ppla-pplb/p) |
| Elgin L42DT | "EPL, ZPL e TSPL" | USB, RS-232 | [product page](https://www.elgin.com.br/impressora-de-etiqueta-termica-direta-l42dt-compacta-usb-rs-232/p) |
| Elgin TT042 Plus | "ZPL, DPL, EPL, TSPL" | USB, RS-232, Ethernet | [manual](https://content.elgin.com.br/assets/arquivos/portal-apoio-novo/C17733D114132FD0C1AE6AFFB6651D14.pdf) |
| TSC TE200 | TSPL-EZD, "Compatible to EPL, ZPL, ZPL II, DPL" | USB; Ethernet or serial options | [datasheet](https://fs.tscprinters.com/system/files/te200_en_datasheet_web_20251030.pdf) |
| Honeywell PC42t Plus | DP, DPL, "ZSim2 (ZPL-II)", EPL | USB; serial or Ethernet option | [datasheet, pt-BR](https://prod-edam.honeywell.com/content/dam/honeywell-edam/sps/ppr/pt-br/localized/data-sheets/sps-ppr-pc42t-desktop-printer-data-sheet-pt-br.pdf) |

Argox's manual calls PPLA, PPLB and PPLZ "printer programming languages" but does not say which vendor each copies ([OS-214EX Pro manual](https://www.argox.com/docfile/usermanual/OS-214EX_Pro_OS-200_Pro_Users_Manual_V1.0_EN.pdf)). The usual reading, that PPLA follows Datamax DPL, PPLB follows EPL and PPLZ follows ZPL, has no Argox source.

**Analysis.** Generating ZPL covers Zebra, Elgin, TSC, Honeywell and, probably, Argox in PPLZ mode. A second PPLA or PPLB generator is needed only for printers that can't be switched.

### Route 1: the browser's print dialog

The page renders the label as HTML or PDF, sized with CSS `@page { size: 50mm 30mm }` ([MDN](https://developer.mozilla.org/en-US/docs/Web/CSS/Reference/At-rules/@page/size); Baseline 2024), and the Windows printer driver rasterizes it. Free Windows drivers exist for Argox, Elgin, Bematech and Zebra ([Drivers by Seagull](https://www.bartendersoftware.com/support/downloads/drivers)).

- **A dialog per label.** Without configuration, someone has to click through the dialog for every label.
- **Silent printing policy.** Chrome 144 added the `SilentPrintingEnabled` policy, which "immediately closes print preview window when opened and prints to the default printer with default options" ([policy definition](https://github.com/chromium/chromium/blob/main/components/policy/resources/templates/policy_definitions/Miscellaneous/SilentPrintingEnabled.yaml)). Edge has it on Windows and macOS from 144 ([Edge docs](https://learn.microsoft.com/en-us/deployedge/microsoft-edge-policies/silentprintingenabled)). The older `--kiosk-printing` switch does the same ([`chrome_switches.h`](https://github.com/chromium/chromium/blob/main/chrome/common/chrome_switches.h)).
- **Silent printing goes to the default printer.** The policy applies to the whole browser, so every print, an A4 report included, goes to the default printer (analysis). `DefaultPrinterSelection` only picks which printer is the default ([policy definition](https://github.com/chromium/chromium/blob/main/components/policy/resources/templates/policy_definitions/Printing/DefaultPrinterSelection.yaml)).
- **Analysis.** It works with any brand and needs no install beyond the driver. Silent printing fits a station that prints only labels. Small barcodes go through the driver's rasterizer instead of the printer's own barcode engine; at 203 dpi that needs testing.

### Route 2: the backend sends ZPL to port 9100

Zebra printers listen for raw jobs on TCP 9100 by default ([Zebra `ip.port`](https://docs.zebra.com/us/en/printers/software/zpl-pg/r-sgd-wireless-sgd-wireless-commands/r-sgd-wireless-ip-port.html)), and Argox does too ("the default listen port is 9100", [OS-214EX Pro manual](https://www.argox.com/docfile/usermanual/OS-214EX_Pro_OS-200_Pro_Users_Manual_V1.0_EN.pdf)).

- **The cloud can't reach a lab LAN printer.** It needs a site-to-site VPN or a relay on the LAN (analysis).
- **The page can't do it either.**
  - It has no raw TCP. Direct Sockets is limited to Isolated Web Apps, which install "on enterprise-managed ChromeOS devices only" ([Chrome Platform Status](https://chromestatus.com/feature/5146307550248960); [IWA introduction](https://developer.chrome.com/docs/iwa/introduction)).
  - An `http://192.168.x.x` request from an HTTPS page is mixed content unless Chrome's Local Network Access permission is granted ([below](#chrome-local-network-access)).

### Route 3: the printer connects out (Zebra Weblink)

Zebra printers on full Link-OS can open a WebSocket *out* to a server we run, so the cloud backend prints with no VPN and no PC agent.

- **The connection.** The printer sends `GET /yourendpoint` with `Sec-WebSocket-Protocol: v1.weblink.zebra.com`. "The printer only supports Binary frames". The server opens a raw channel that "will accept any ZPL, CPCL, SGD commands" ([Zebra application note: creating a Weblink endpoint](https://www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/application-notes/appnotes-creating-weblink-endpoint-rev1-english.pdf)).
- **Configuration.** Set `weblink.ip.conn1.location` to `https://<host>/zebra/weblink/` on each printer.
- **Certificates.** "Zebra is not a trusted certificate authority". Zebra "strongly recommends" a public or self-signed certificate, whose chain "must be deployed to every printer".
- **Server.** Zebra ships a `zebra.war` for Tomcat 10 or later. "SHA-1 support has been removed in Link-OS v7.2" ([Link-OS SDK 2.14 web services](https://techdocs.zebra.com/link-os/2-14/webservices/content/index.html)).
- **Zebra's hosted version.**
  - SendFileToPrinter covers "all Link-OS printers except for Link-OS basic printers". It is free for 100 calls a day and US$0.01 a call above that, and Zebra is "transitioning" it to the Zebra Cloud Print API ([SendFileToPrinter](https://developer.zebra.com/apis/sendfiletoprinter)).
  - Cloud Print API 1.0 needs "Link-OS v5.0 (or later)" on ZD400/500/600, ZT200–600 and similar, a "Zebra Nucleus tenant", and a per-printer license whose price comes from Zebra Sales ([release notes](https://www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/release-notes/0002/Zebra-Cloud-Print-API-1.0-Release-Notes.pdf); [flyer](https://www.zebra.com/content/dam/zebra_dam/en/flyer/cloud-print-api-flyer-sftp-comparison-en-us.pdf)).
- **Which printers.**
  - **ZD220/ZD230 are out.** They are Link-OS Basic ([ZD200 FAQ](https://www.zebra.com/content/dam/zebra_dam/en/faq/zd200-series-faq-a4-en-us.pdf)), and SendFileToPrinter excludes Link-OS Basic. That Weblink itself is missing on them comes only from a [forum reply](https://developer.zebra.com/content/cant-connect-zd220-internet) by a non-staff user.
  - **The ZD421 class is the floor.** This route means buying a ZD421, ZD621 or ZT231.
- **No other brand.** No Argox or Elgin equivalent turned up.

### Route 4: Zebra Browser Print

A Zebra application that "allows web pages to communicate with Zebra Printers" ([user guide v1.3](https://www.zebra.com/content/dam/zebra_new_ia/en-us/solutions-verticals/product/Software/Printer%20Software/Link-OS/browser-print/zebra-browser-print-user-guide-v1-3-en-us.pdf)).

- **Platforms.** Windows and macOS. A separate Android build installs as an APK from unknown sources ([Android guide](https://www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/guide/software/zebra-browser-print-user-guide-v1-3-2-android-en-us.pdf)).
- **Scope.** ZPL II only, Zebra printers only, USB and network.
- **How the page reaches it.** It "cannot run when any other program is using the computer's 9100 or 9101 ports". Each site must be approved once and is kept in an "Accepted Hosts" list.
- **Availability.** No price is shown; it is "available on a request basis" ([download page](https://www.zebra.com/us/en/support-downloads/software/printer-software/browser-print.html)). The newest guide is from 2020, and maintenance status is unknown.
- **Chrome's loopback prompt.** As a localhost server, it now falls under Chrome's [Local Network Access prompt](#chrome-local-network-access).

### Route 5: QZ Tray

- **License and scope.**
  - The source is LGPL-2.1 ([LICENSE](https://github.com/qzind/tray/blob/master/LICENSE.txt)); the latest release is v2.3.0, from 2026-09-11 ([release](https://github.com/qzind/tray/releases/tag/v2.3.0)).
  - It runs on Windows, macOS and Linux, and listens on `wss://localhost:8181` ([FAQ](https://qz.io/docs/faq)).
  - It prints raw "ZPL, EPL, ESC/POS, ESC/P, DPL, PGL, and SBPL" plus PDF, HTML and images.
  - It also does serial, with a Mettler Toledo example ([serial docs](https://qz.io/docs/serial)), HID ([HID docs](https://qz.io/docs/hid)) and raw USB ([USB docs](https://qz.io/docs/usb)). One install could cover printers, balances and scanners.
- **The free version prompts.** "QZ Tray is free", but without signing it shows a prompt per action. Removing the prompts needs "message signing for each print function. This also applies to pop-ups for serial, usb or network communications" ([signing docs](https://qz.io/docs/signing)). The signature is made on our backend with a certificate QZ sells, or with our own root installed on each PC as `override.crt`.
- **Price** (store, 2026-10-20).
  - Premium Support, which includes a one-year trusted key pair, is US$749 a year, or US$599–699 a year on multi-year terms ([store](https://buy.qz.io/Premium-Support-_p_13.html)).
  - A white-label "Company Branded" tier is US$3,499 a year ([store](https://buy.qz.io/Company-Branded-Premium-Support_p_19.html)).
  - "A single license will suffice for 99% of organizations" ([FAQ](https://qz.io/docs/faq)).

### Route 6: WebUSB to the printer

- **The OS driver blocks it.** WebUSB requires that "a device should not already have a driver claiming the interface the page wants to control". On Windows, a page can only open a device bound to WinUSB, which "needs to be loaded" ([Building a device for WebUSB](https://developer.chrome.com/docs/capabilities/build-for-webusb)). A USB label printer on Windows is held by its printer driver.
- **One forum report.** It confirms the result: WebUSB to a ZD621 "prints successfully on Mac OS" but fails on Windows with "Access denied" ([Zebra developer forum](https://developer.zebra.com/content/domexception-failed-execute-open-usbdevice-access-denied); no Zebra staff answer).
- **Not blocked by class.** The printer class (0x07) is not in WebUSB's protected list ([WebUSB spec](https://usb.spec.whatwg.org/)).
- **Analysis.** Swapping the driver for WinUSB would break normal printing, so this route only fits a printer dedicated to the web app. On Android, a device with no built-in OS driver is open to the browser (same Chrome article).

## 3. Precision balances

### How balances connect

Almost every balance offers two kinds of link, and only one of them supports monitored weighing.

- **Keyboard emulation.** The balance types the number into whatever window has focus. Mettler's "PC-DIR." mode "sends the weight value without the unit" ([MS-S/MS-L operating instructions](https://www.mt.com/content/dam/non-indexed/po/labtec/02_Precision/04_MS/OI_MS-S_MS-L_Analytical_Precision_EN.pdf), pp. 40, 81). Sartorius has "PC spreadsheet format" and "PC text format", sent over USB "as PC Keyboard without Additional Driver" ([Secura/Quintix/Practum manual](https://api.sartorius.com/document-hub/dam/download/21625/Manual_Secura_Quintix_Practum_WSE6004-e181008.pdf), p. 36). Shimadzu's "WindowsDirect" is keyboard emulation over RS-232 using Windows "Serial Keys" or a Shimadzu tool ([TW/TX/TXB manual](https://www.shimadzu.com/an/sites/shimadzu.com.an/files/pim/pim_document_file/manuals/24012/wind-3e_twtx.pdf), pp. 116–118; successor [Balance Keys](https://shimadzu.com/an/products/balances/balances-software/balance-keys/index.html)).
- **Serial request/response.** The balance has a real RS-232 port or a USB virtual COM port, and its protocol carries a stability flag and accepts commands.
  - Mettler's MS-S USB is "CDC (Communication Device Class) serial port emulation" (MS-S/MS-L manual, p. 91).
  - Sartorius's "PC - SBI" mode is a "CDC Virtual Com Port" (Secura manual, p. 36).
  - Windows loads its built-in usbser.sys for CDC devices: "You don't need to write your own INF file" ([Microsoft Learn](https://learn.microsoft.com/en-us/windows-hardware/drivers/usbcon/usb-driver-installation-based-on-compatible-ids)).

**Analysis.** Keyboard emulation needs no code and works anywhere a keyboard does. But it is one-way, usually carries no stability flag, sometimes drops the unit, and goes to whichever field has focus. Only the serial path can check stability and read the balance's identity, which is what "pesagem monitorada" needs.

### Protocols by vendor

| Vendor | Protocol and stability flag | Commands worth knowing | Interfaces and defaults |
| --- | --- | --- | --- |
| Mettler Toledo | MT-SICS. `S S     100.00 g`, where the second field is "S Stable" or "D Unstable ('D' for Dynamic)" (p. 5, p. 77) | `S` stable weight, `SI` immediate, `SIR` repeat, `Z`, `T`, `@` reset (answers with the serial number), `I4` serial number; send `I0` to list supported commands (p. 9) | LA: two serial ports and a USB device fixed to "MT-SICS Host" ([MT-SICS LA reference](https://mt.com/content/dam/non-indexed/po/labtec/12_Miscellaneous/RM-MT-SICS-LA-en.pdf), p. 64). MS-S RS-232 default 9600 8N1, XON/XOFF. `M89` switches to "Sartorius 22 character output format" (p. 67) |
| Sartorius | SBI: 16 characters (sign, weight, unit, CRLF), or 22 with an ID prefix. No explicit stability character; the printout settings say an unstable value has no unit symbol. Whether the PC interface follows that is unverified | `Esc P` print, `Esc T` tare, `Esc V` zero, `Esc x2_` serial number, `Esc x5_` balance ID (Secura manual, p. ~89) | Secura/Quintix: USB Mini-B with PC-SBI (virtual COM), PC-xBPI, two keyboard modes; RS-232 via an adapter cable; 9600 8O1 (Secura manual). Cubis II: RS-232, USB virtual COM with SBI default ([datasheet](https://www.sartorius.com/download/64168/data-cubis-ii-e-data.pdf)). Entris II/Quintix Pro: [interface note](https://api.sartorius.com/document-hub/dam/download/295216/Interface-Description-Entris-II-Quintix-Pro-Technical-Note-en-Sartorius.pdf) |
| Shimadzu (TW/TX/TXB) | Sign, 8 digits, unit, CR, with an optional leading "S (53H)" when stable and "U (55H)" when unstable (pp. 123–124) | `D05` single, `D07` single with stability, `D08` single when stable, `D01`–`D03` continuous, `D09` cancel, `T` tare (pp. 125–126) | RS-232C. Presets MODE.1 (Shimadzu, 1200 8N1) and MODE.3–5 emulating Mettler, Sartorius and A&D (p. 132) ([TW/TX/TXB manual](https://www.shimadzu.com/an/sites/shimadzu.com.an/files/pim/pim_document_file/manuals/24012/wind-3e_twtx.pdf)) |
| Marte | Analytical models are Shimadzu (ATX224R: "RS232C (DB9M) e USB (Tipo B)", "Função Windows Direct", [product page](https://www.marte.com.br/balanca-analitica-0-0001g-220g-4-casas-calibracao-interna-automatica-atx224-r-shimadzu-inmetro/p)). The AD line's protocol has no public manual | — | AD500: "saída RS232 programável"; accessories include RS-232 to USB and RS-232 to TCP/IP converters ([product page](https://www.marte.com.br/balanca-semi-analitica-3-casas-0-001g-510g-ad500-marte-inmetro/p)) |
| Gehaka | Output on key press, or continuous, with or without stability (TECLA I/E, CONTI I/E) ([AG200 manual](https://www.gehaka.com.br/produtos/linha-pesagem/balancas-analiticas/ag200/manual_ag200.pdf), p. 19) | `Z` zero/tare, `P` "Envia leitura atual", `C` header with balance data, CRLF-terminated | RS-232C only; factory "4.800 bps", 8N1. BK line: "Serial RS232C em todos os modelos", USB adapter optional ([BK300](https://www.gehaka.com.br/produtos/linha-pesagem/balancas-semianaliticas/bk300)) |
| BEL Engineering | Continuous string with a stability character in position 13; on-demand and GLP modes ([MG/LG manual](https://www.belengineering.com/?mdocs-file=15040), §11.4.1) | Key codes: `T` tare, `C` calibrate, `E`, `M`, `O` | RS-232 only, 1200–9600 8N1; "Serial to USB converter (E1002)" sold as an accessory ([M214A](https://www.belengineering.com/product/m214a/)) |
| Ohaus Explorer | "OHAUS / SICS = MT-SICS / ST-SICS"; the Ohaus format marks unstable values with "?" | `IP`, `P`, `SP`, `T`, `Z`, `PSN` | RS-232, USB virtual COM with Ohaus's driver (9600 8N1), Ethernet option ([instruction manual](https://dmx.ohaus.com/WorkArea/downloadasset.aspx?id=28112), EN-115, EN-158–163) |

**Analysis.**

- **Parsers.** MT-SICS (also spoken by Ohaus and emulated by Shimadzu presets), SBI, Shimadzu's D-commands and Gehaka's would cover every model above except Marte's AD line, whose protocol needs a bench test.
- **Adapters.** The Brazilian brands checked are RS-232 only, so each needs a USB adapter.
- **Identity.** Mettler, Sartorius and Ohaus answer with their serial number on request, so the app can record which balance made each weighing.

What the regulation asks of a weighing, from [RDC 67/2007](https://bvsms.saude.gov.br/bvs/saudelegis/anvisa/2007/rdc0067_08_10_2007.html):

- **The compounding record.** It must carry the "lote de cada matéria-prima, fornecedor e quantidade pesada" and the "nome e assinatura dos responsáveis pela pesagem" (Annex I, 8.4).
- **Hormones.** "Na pesagem para manipulação deve haver dupla checagem, sendo uma realizada pelo farmacêutico, com registro dessa operação" (Annex III, 2.15.6).
- **Sterile preparations.** The weighing room must be ISO class 7 (Annex IV, 4.14.1).

### Web Serial on the lab PCs

- **Support.** Desktop Chrome and Edge since 89 on Windows, macOS, Linux and ChromeOS ([Chrome Platform Status](https://chromestatus.com/feature/6577673212002304)). Bluetooth RFCOMM serial was added in 117 ([status](https://chromestatus.com/feature/5686596809523200)). Firefox 151 added it on desktop behind a "site permission add-on" ([Firefox 151 notes](https://github.com/mdn/content/blob/main/files/en-us/mozilla/firefox/releases/151/index.md)). Safari doesn't have it, and WebKit's position is "oppose" ([standards-positions #199](https://github.com/WebKit/standards-positions/issues/199)).
- **Permission.**
  - `requestPort()` rejects without "transient activation", so the first pick needs a click. The API is secure-context only, and its Permissions-Policy token is `serial` ([Web Serial spec](https://serial.spec.whatwg.org/)).
  - `getPorts()` later returns ports the user already allowed, with no gesture. `forget()` (Chrome 103) revokes a grant ([Chrome Platform Status](https://chromestatus.com/feature/5145811414417408)).
- **Whether a grant survives a replug** ([`serial_chooser_context.cc`](https://github.com/chromium/chromium/blob/main/chrome/browser/serial/serial_chooser_context.cc)).
  - On Windows, Chrome keeps it when the port has a Windows device instance ID.
  - On Linux, ChromeOS and Android, it needs a USB serial number. macOS also needs the driver name.
  - Otherwise the grant is ephemeral and is revoked on unplug.
  - Without a USB serial number, Windows builds the instance ID from location: "If a device is moved to a new parent (plugged into a different port…), that id changes" ([Microsoft](https://devblogs.microsoft.com/windows-music-dev/the-importance-of-including-a-unique-iserialnumber-in-your-usb-midi-devices/)). So moving a cheap adapter to another USB socket means picking it again (inference, not tested).
- **Pre-granting by policy.** `SerialAllowUsbDevicesForUrls` grants an origin access by USB vendor ID and product ID, and "overrides … the user's preferences" ([Chrome policy templates](https://chromeenterprise.google/static/json/policy_templates_en-US.json); Edge: [docs](https://learn.microsoft.com/en-us/deployedge/microsoft-edge-browser-policies/SerialAllowUsbDevicesForUrls)). Chromium checks the policy before stored grants, so policy-granted ports come back from `getPorts()` with no picker and no serial-number requirement ([`serial_service.cc`](https://github.com/chromium/chromium/blob/main/content/browser/serial/serial_service.cc)).
- **Reconnect.**
  - When a granted port comes back, the browser fires `connect`; when it goes away, `disconnect`. A removed port's `readable` becomes null (spec). `SerialPort.connected` arrived in Chrome 130 ([status](https://chromestatus.com/feature/5118102654418944)).
  - So the page must, at load, call `getPorts()` and match by `getInfo()`; on `connect`, call `open()` again and restart its read loop; and, for an ephemeral grant, show a "reconnect balance" button that calls `requestPort()`.
  - `getInfo()` returns only USB vendor and product IDs (spec), so two identical adapters look the same to the page. Asking each balance for its serial number (MT-SICS `I4`, SBI `Esc x2_`) tells them apart (analysis).

## 4. Tablets

| Capability | Lab PC (Chrome/Edge on Windows) | Android tablet (Chrome) | iPad (any browser) |
| --- | --- | --- | --- |
| Keyboard-wedge scanner | USB or Bluetooth | Bluetooth HID ([DS2278 guide](https://www.zebra.com/content/dam/support-dam/en/documentation/unrestricted/guide/product/ds2278-prg-en.pdf), p. 6-37) | Bluetooth HID (same guide) |
| Camera + ZXing wasm | yes (webcam) | yes | yes, in a Safari tab ([MDN data](https://github.com/mdn/browser-compat-data/blob/main/api/MediaDevices.json)) |
| Native `BarcodeDetector` | no (Windows and Linux) | yes, 83+, needs Play Services | behind a flag only |
| Web Serial | yes, 89+ | Bluetooth RFCOMM 138+; wired 148+ where the OS exposes ports | no |
| WebUSB | yes, 61+, but OS drivers hold printers | yes, 61+ | no |
| WebHID | yes, 89+ | "planned for Chrome 157" | no |
| Web Bluetooth (BLE) | yes, 70+ | yes, 56+ | no |
| Page calls a localhost or LAN bridge | yes, after the LNA prompt or policy | yes, after the LNA prompt or policy | blocked: WebKit treats `http://127.0.0.1` as mixed content |

Sources for the table:

- **Web Serial on Android.**
  - Bluetooth: "Chrome on Android now supports Web Serial API over Bluetooth RFCOMM" ([Chrome 138 release notes](https://developer.chrome.com/release-notes/138); [status](https://chromestatus.com/feature/5139978918821888)).
  - Wired: "Web Serial API on Android", Android milestone 148 ([status](https://chromestatus.com/feature/6043992171085824); [Chrome 148 release notes](https://developer.chrome.com/release-notes/148)). In the [blink-dev Intent to Ship](https://groups.google.com/a/chromium.org/d/msgid/blink-dev/69c2e4a0.050a0220.38bee9.0000.GAE%40google.com) thread, the Chrome engineers say this "requires OS-level support" that "will be available on Android 17". Only devices configured to expose wired serial ports return them; the rest get empty lists. The OS API is [`SerialManager`](https://developer.android.com/reference/android/hardware/serial/SerialManager), "Added in API level 37".
- **WebUSB:** [status](https://chromestatus.com/feature/5651917954875392).
- **WebHID:** "Support on Android is planned for Chrome 157" ([status](https://chromestatus.com/feature/5172464636133376), edited 2026-10-07). Chrome 157 ships on 2026-11-03, and its [beta post](https://developer.chrome.com/blog/chrome-157-beta) doesn't mention it.
- **Web Bluetooth:** [status](https://chromestatus.com/feature/5264933985976320); [implementation status](https://github.com/WebBluetoothCG/web-bluetooth/blob/main/implementation-status.md).
- **WebKit's refusals.** WebKit opposes WebUSB ([#68](https://github.com/WebKit/standards-positions/issues/68)), Web Serial ([#199](https://github.com/WebKit/standards-positions/issues/199)), WebHID ([#510](https://github.com/WebKit/standards-positions/issues/510)) and Web Bluetooth ([#570](https://github.com/WebKit/standards-positions/issues/570)). It lists all four among features "we have decided to not yet implement due to fingerprinting, security, and other concerns" ([WebKit tracking prevention](https://webkit.org/tracking-prevention/)).
- **Every iPad browser is WebKit.** "Apps that browse the web must use the appropriate WebKit framework and WebKit JavaScript", with an exception only "for the EU and Japan" ([App Store Review Guideline 2.5.6](https://developer.apple.com/app-store/review/guidelines/); [alternative browser engines](https://developer.apple.com/support/alternative-browser-engines/)). Chrome on an iPad in Brazil has Safari's gaps.
- **The iPad and the loopback bridge.** [WebKit bug 171934](https://bugs.webkit.org/show_bug.cgi?id=171934), "Don't treat loopback addresses … as mixed content", is still open. A 2026 comment says WebKit "has not shipped a similar defense" to Chrome's.

**Analysis.**

- **iPads.** An iPad is a form-filling and scanning device. A Bluetooth scanner pairs as a keyboard: "A link should be established allowing scanning into any application with keyboard entry" (DS2278 guide, p. 6-37). With a hardware keyboard attached, iPadOS hides the on-screen keyboard; Zebra's "Apple iOS Virtual Keyboard Toggle" opens it with a double trigger press (p. 6-16).
- **Android tablets.** They could also weigh, either with a Bluetooth SPP balance or with a wired one once Android 17 devices expose serial ports. Neither is a dependable target for a first version.
- **Weighing at the bench.** If a lab worker must weigh while holding a tablet, a lab PC or agent has to read the balance and push readings through the backend to the tablet.

## 5. A local bridge

### Two shapes

- **(a) The agent listens on localhost**, and the page calls `wss://localhost:<port>`. QZ Tray uses ports 8181–8484 ([`qz-tray.js`](https://github.com/qzind/tray/blob/master/js/qz-tray.js#L87-L98)), and Zebra Browser Print uses 9100/9101. The page must be open on that PC, and Chrome's loopback rules apply.
- **(b) The agent connects out** to the backend over WebSocket, gRPC or MQTT, and the backend relays commands to it. PrintNode works this way: "The PrintNode Client makes outbound connections on ports 443 and 6123" ([FAQ](https://www.printnode.com/en/faq)). So does Microsoft's Universal Print connector, which "fetches print jobs from Universal Print and delivers them to their target printer" ([Microsoft Learn](https://learn.microsoft.com/en-us/universal-print/fundamentals/universal-print-connector-overview)).

**Analysis.** Shape (b) avoids the loopback prompt, mixed content and an exposed localhost port. Any device that can reach the backend can use it, an iPad included. The costs are the round trip through the cloud and a registry, on the backend, of which agent serves which bench.

### Chrome Local Network Access

Private Network Access (preflight headers) has been replaced by a permission prompt, Local Network Access (LNA):

- **Chrome 142** (2025-10-28) gated "any request from a public website to a local IP address or loopback" behind a prompt, on secure contexts only. "If granted, the permissions additionally relaxes mixed content blocking for local network requests" ([Chrome 142 release notes](https://developer.chrome.com/release-notes/142)).
- **Chrome 145** split the permission in two, `local-network` and `loopback-network` ([Chrome 145 release notes](https://developer.chrome.com/release-notes/145)).
- **Chrome 147** (2026-04-07): "WebSockets connections to local addresses now trigger permission prompts" ([Chrome 147 release notes](https://developer.chrome.com/release-notes/147)). This covers QZ Tray and any localhost agent of ours.
- **The spec** gates access by permission "rather than via preflight requests", so `Access-Control-Allow-Private-Network` is no longer used ([LNA spec](https://wicg.github.io/local-network-access/), draft of 2026-08-07). Mixed-content relief applies only when Chrome knows in advance that the target is local: a private IP literal, `.local`, or `targetAddressSpace` ([Chrome blog](https://developer.chrome.com/blog/local-network-access)).
- **Pre-granting by policy.**
  - `LoopbackNetworkAllowedForUrls` and `LocalNetworkAllowedForUrls` (146 and later, desktop and Android) list the requesting site's origin; `LocalNetworkAccessAllowedForUrls` (139 and later) is the older combined form ([policy definitions](https://github.com/chromium/chromium/tree/main/components/policy/resources/templates/policy_definitions/LocalNetworkAccessSettings)).
  - Edge documents the same policies ([Edge docs](https://learn.microsoft.com/en-us/deployedge/microsoft-edge-browser-policies/loopbacknetworkallowedforurls)).
  - The old PNA policies ended at 137 ([PNA policy definitions](https://github.com/chromium/chromium/tree/main/components/policy/resources/templates/policy_definitions/PrivateNetworkRequestSettings)).

### Mixed content

- **The spec.** Secure Contexts treats 127.0.0.0/8 and ::1 as "Potentially Trustworthy" ([spec](https://w3c.github.io/webappsec-secure-contexts/#is-origin-trustworthy)), so an HTTPS page may call `http://127.0.0.1` or `ws://127.0.0.1` ([MDN: mixed content](https://developer.mozilla.org/en-US/docs/Web/Security/Mixed_content)).
- **Chrome** has allowed this since 2016 ([Chromium commit](https://chromium.googlesource.com/chromium/src.git/+/130ee686f%5E%21)).
- **WebKit still blocks it** ([bug 171934](https://bugs.webkit.org/show_bug.cgi?id=171934); [bug 281149](https://bugs.webkit.org/show_bug.cgi?id=281149)).
- **LAN IPs.** A LAN IP such as `http://192.168.1.50` is not potentially trustworthy. It is blocked as mixed content unless the Chrome LNA permission is granted.

### Securing a localhost server

Any website the user opens can reach a localhost server.

- **The Zoom case.** CVE-2019-13450: "any web site can interact with the Zoom web server on localhost port 19421 or 19424" ([CVE record](https://www.cve.org/CVERecord?id=CVE-2019-13450)).
- **DNS rebinding.** CVE-2018-5702: Transmission relied on a custom header, which allowed RPC commands "in conjunction with a DNS rebinding attack" ([CVE record](https://www.cve.org/CVERecord?id=CVE-2018-5702)). The fix checks the `Host` header against allowed names ([Transmission RPC spec](https://github.com/transmission/transmission/blob/main/docs/rpc-spec.md#222-dns-rebinding-protection)).
- **WebSockets skip CORS.** The server must check `Origin`: "If the server does not validate the origin, it will accept connections from anywhere" ([RFC 6455](https://www.rfc-editor.org/rfc/rfc6455#section-10.2)).
- **The LNA prompt doesn't absolve the vendor.** The LNA spec says vendors "should not consider themselves absolved of responsibility" ([LNA spec](https://wicg.github.io/local-network-access/)).

**Analysis.** A localhost agent of ours would need at least an `Origin` allowlist, a `Host` check, and a pairing token the backend issues per workstation. Shape (b) needs none of these, because nothing listens.

### Existing products

| Product | Shape | Covers | Price | Platforms |
| --- | --- | --- | --- | --- |
| QZ Tray | (a) localhost WSS | raw and PDF printing, serial, HID, USB | free with prompts; US$749/yr to sign ([store](https://buy.qz.io/Premium-Support-_p_13.html)) | Windows, macOS, Linux |
| Zebra Browser Print | (a) localhost 9100/9101 | ZPL to Zebra printers | on request ([page](https://www.zebra.com/us/en/support-downloads/software/printer-software/browser-print.html)) | Windows, macOS, Android APK |
| PrintNode | (b) outbound 443/6123 | raw and PDF printing; "serial and USB weighing scales" ([home](https://www.printnode.com/en)) | free (50 prints, 1 computer); US$9/mo (5,000 prints, 3 computers); US$29; US$99 ([pricing](https://www.printnode.com/en/pricing)) | client on Windows, macOS, Linux; "Mobile devices … not currently supported" |
| Neodynamic JSPrintManager | (a) | printing; serial on desktop, not Android | US$249.90 per web app plus US$249.90 per web server, perpetual ([buy page](https://www.neodynamic.com/products/printing/js-print-manager/buy/)) | Windows, macOS, Linux, Android app |
| BarTender Cloud | cloud print | label design and printing; REST and "Direct cloud printing" from the Automation tier | from US$390/yr (Essentials) or US$1,750/yr (Automation) ([pricing](https://www.bartendersoftware.com/product/pricing)) | — |
| Zebra Weblink / Cloud Print API | (b), in the printer itself | ZPL (and PDF via the API) on full Link-OS Zebras | own server: no Zebra fee found; Cloud Print API: per printer, price from sales | printer firmware |

- **PrintNode's scales API.** It reports mass in micrograms, and the data expires after 45 seconds. The vendor's advice is to read scales from the browser over its WebSocket client ([API docs](https://www.printnode.com/en/docs/api/curl)). PrintNode reads scales "which implement the USB HID standard" and "serial scales", not every model ([FAQ](https://www.printnode.com/en/faq)).
- **Balance vendors' PC software only types into fields.** Mettler's EasyDirect Balance ([flyer](https://www.mt.com/dam/P5/labtec/09_Software/EasyDirect-Balance-v1-2.pdf)) and BalanceLink, which sends data to "the cursor" ([flyer](https://www.mt.com/dam/mt_ext_files/Editorial/Generic/6/balancelink_0x0002464400026a70000b741b_files/balancelink_en.pdf)), fill fields or export files. Neither is a bridge a web app can call.
- **Google Cloud Print is gone.** It was "deprecated in January of 2021" ([Google](https://support.google.com/chrome/a/answer/9633006)).

### The cost of writing our own agent

Our backend is Java, so the natural agent is a small Java service.

- **Serial.**
  - [jSerialComm](https://github.com/Fazecast/jSerialComm) is dual Apache-2.0 / LGPL-3.0 ([NOTICE](https://github.com/Fazecast/jSerialComm/blob/master/NOTICE)). Its last release was v2.11.4, on 2025-11-04, and Java 24 and later need `--enable-native-access`.
  - [jssc](https://github.com/java-native/jssc) is LGPL-3.0, with v2.10.4 released 2026-08-24.
- **HID.** [hid4java](https://github.com/gary-rowe/hid4java) is MIT; its last release was 0.8.0, in 2024.
- **Installers.**
  - `jpackage` builds `exe`, `msi`, `pkg`, `dmg`, `deb` and `rpm` with a bundled runtime. It can install the app as a service (`--launcher-as-service`). But "each format must be built on the platform it runs on".
  - It has no auto-updater, only `--win-update-url` metadata ([jpackage, JDK 25](https://docs.oracle.com/en/java/javase/25/docs/specs/man/jpackage.html)). Updates are ours to build.
- **Code signing from Brazil.**
  - Microsoft's Artifact Signing issues Public Trust certificates only to organizations in a listed set of countries, and Brazil isn't one of them. Brazil South does host the service, and the restriction doesn't apply to Private Trust certificates, which are trusted only where the organization deploys them ([quickstart](https://learn.microsoft.com/en-us/azure/artifact-signing/quickstart)).
  - The alternative is an OV certificate, "$150–300/year", whose key must sit on a hardware token or HSM ([Microsoft: code signing options](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/code-signing-options); [CA/B Forum Code Signing BRs](https://cabforum.org/working-groups/code-signing/requirements/) §6.2.7.4.2).
  - For an open-source project there is [SignPath Foundation](https://signpath.org/), which signs under its own name.
  - Even signed, "a newly created binary could still show a SmartScreen warning until its hash or publisher certificate accumulates sufficient evidence" ([SmartScreen reputation](https://learn.microsoft.com/en-us/windows/apps/package-and-deploy/smartscreen-reputation)).
  - On PCs the pharmacy manages, a self-signed certificate deployed as a trusted root by Group Policy avoids all of this (same code-signing page).
- **A browser extension with native messaging** replaces the localhost server with stdin/stdout to a native host. It still needs the native host installed, so it means two installs, and it works on Windows, macOS and Linux only ([Chrome docs](https://developer.chrome.com/docs/extensions/develop/concepts/native-messaging)).
- **Electron or Tauri.** [Electron](https://github.com/electron/electron) (MIT) and [Tauri](https://github.com/tauri-apps/tauri) (Apache-2.0 or MIT) would be the choice only if the agent needed its own UI (analysis).

## Options for AmpolaTech (analysis)

Everything in this section is analysis built on the sections above.

All three options share the same scanner setup: keyboard-wedge scanners set to the ABNT2 layout, an F2 or F8 prefix, AIM IDs and GS1 DataMatrix enabled, read by a document-level `keydown` handler. Camera scanning on tablets uses the ZXing ponyfill. They differ in how balances and printers are reached.

### A. Browser only, with a printer that connects out

- **Balances.** Web Serial on the lab PCs, pre-granted with `SerialAllowUsbDevicesForUrls` by Group Policy, so no picker and no re-pick after a replug. Balances must have a virtual COM port or RS-232 plus an adapter, and a request/response protocol. We write two to four protocol parsers in TypeScript.
- **Printers.** Zebra ZD421, ZD621 or ZT231 on the lab LAN with Weblink pointed at a WebSocket endpoint in our backend, which sends ZPL. Any other brand prints through the browser's print dialog, ideally from a station that prints only labels with `SilentPrintingEnabled`.
- **Tablets.** Forms and scanning only. A tablet can trigger a label, because printing goes through the backend. It can't weigh.
- **Cost.**
  - Nothing to install, sign or update on the PCs.
  - Printer choice is limited to Zebra's mid-range or better.
  - The PCs need managed Chrome or Edge policies.
  - The backend holds long-lived printer sockets; with more than one instance, a job must be routed to the instance holding that printer's socket.
  - Weighing works only at a PC with the page open, and the readings come from the browser, so the backend has to trust the client's report.

### B. Our own agent, connecting out

- **What it is.** A small Java service on each lab PC (jpackage MSI, run as a Windows service). It opens one outbound connection to the backend, authenticated by a per-workstation token issued at enrollment.
- **Balances.** The agent reads them with jSerialComm and pushes each reading, with the balance's serial number, to the backend.
- **Printers.** It sends raw ZPL, PPLA or PPLB to LAN printers on 9100, or to USB printers through the Windows spooler.
- **Tablets.** Any screen can trigger a print or watch a weighing, iPads included, because both go through the backend.
- **Cost.**
  - We build and keep an installer, an update mechanism, code signing (an OV certificate on a token, or a Group Policy-trusted root on managed PCs) and enrollment.
  - SmartScreen warnings until the publisher builds reputation.
  - The agent becomes the thing support has to check first.
  - jSerialComm's last release was nearly a year ago.
- **Gains.**
  - Any printer brand.
  - No browser permission prompts and no localhost port.
  - Weighings captured server-side from the device, which makes the "quantidade pesada" record and the pharmacist's double check stronger evidence than a number the browser reported.

### C. Buy the bridge

- **QZ Tray** (shape a). Install it on each PC and sign requests on our backend for US$749 a year. It covers printers of any language plus serial and HID, so balances too.
  - The page must be open on the PC that has the device.
  - Chrome shows the loopback prompt unless `LoopbackNetworkAllowedForUrls` is set.
  - Tablets get nothing from it.
- **PrintNode** (shape b). Install its client and call its REST API from our backend, at US$9–99 a month depending on volume.
  - Tablets can trigger prints and see scale data through our backend.
  - Its scale support covers USB HID and some serial scales, not every protocol in section 3.
  - Labels, which carry patient names, and weighings pass through a third party's cloud. That needs a data-protection review.
- **What both save.** Building an agent. What they cost: a recurring fee, and dependence on a vendor for something in the critical path of production.

### What decides between them

1. **Do tablets need to weigh or print at the bench?** If only forms and scanning, A is enough. If tablets must weigh, it is B or PrintNode.
2. **Which printers does the pharmacy already have, or is willing to buy?** Zebra full Link-OS favors A. Argox or Elgin in PPLA or PPLB favors B or C.
3. **Are the lab PCs managed?** Without Group Policy for the Chrome policies and the agent's install, A loses its no-picker Web Serial and silent printing, and B loses easy signing and deployment.

A path that keeps options open: ship v1 on A for scanners and Zebra printing. Put device access behind one interface on the frontend (`readWeight()`, `printLabel()`), so a later move to an agent, for weighing from tablets, changes the backend and that interface's implementation rather than the screens.

## Not verified

- **Ctrl+] in an input.** Whether it reaches an `<input>` as nothing (expected) or as some character in Chrome/Edge on Windows, and how it behaves when scanner and OS both use ABNT2. Needs a bench test with a real scanner.
- **FN1 substitution for GS1 DataMatrix.** Whether Zebra's FN1 substitution applies to it, and what enabling "GS1 Data Matrix" changes in the DS2208's output.
- **Honeywell's full user guides.** Their access is restricted, so the Brazil layout code, HID POS and USB serial modes are unchecked.
- **WebHID with a real HID POS scanner** in Chrome. The "allowed" reading comes from Chromium source only.
- **Wired Web Serial on Android tablets.** Whether any shipping tablet exposes wired serial ports to Chrome 148+, which depends on Android 17 and the device maker.
- **WebHID on Android.** Whether it actually ships in Chrome 157.
- **Barcode quality through a driver.** Whether 203 dpi printers render small DataMatrix codes reliably when the barcode is drawn by the page and rasterized by the driver, rather than generated by the printer from ZPL.
- **Argox's emulation mapping.** Which vendor language PPLA, PPLB and PPLZ each copy. Godex and Bematech label-printer languages weren't found either.
- **Zebra pricing and maintenance.** Weblink support on Link-OS Basic printers (ZD220/ZD230) beyond a non-staff forum reply. The Cloud Print API's per-printer price. Whether Browser Print is still maintained.
- **QZ Tray's own root.** Whether using `override.crt` with our own certificate is allowed without a paid plan.
- **Unread balance documents.**
  - Marte's AD-line serial protocol: no manual was found online.
  - Mettler XPR/XSR interfaces and MT-SICS over Ethernet: mt.com product pages refused automated fetches.
  - Whether Mettler XPR or Sartorius Cubis II offer a keyboard mode on USB.
  - Whether Shimadzu ATX/ATY USB is a virtual COM port or a keyboard.
- **SBI stability on the PC interface.** Whether an unstable value really comes without a unit symbol, as the printout setting says.
- **Persistence edge cases.**
  - Whether Chrome's LNA permission grant persists across sessions like other site permissions.
  - How Chrome persists Web Serial grants for built-in COM ports on Linux.
- **Firefox and loopback.** Firefox's mixed-content treatment of loopback, from Mozilla's own docs.
