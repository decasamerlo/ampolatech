# A hosted web app, with lab hardware reached from the browser

AmpolaTech is a web app hosted in the cloud and used in the browser, on the lab's Windows PCs and on Android tablets, rather than desktop software installed in each pharmacy. Pharmacy staff use it from different devices, and the pharmacy does not need it to work without internet, which was desktop software's main advantage.

Lab hardware will be reached from the browser, not through software installed on the lab PCs: barcode scanners in keyboard mode, labels rendered as PDF documents, and precision balances through Web Serial in Chrome or Edge. The first version only lets users download labels as PDF; printing from the page and scanning come later. The survey behind this is [lab-hardware-from-the-browser.md](../research/lab-hardware-from-the-browser.md).

## Considered options

- **Desktop software with a local database.** It works offline and can talk to any device, but it has to be installed and updated on every PC, and the tablets would need a second app.
- **A local agent on each lab PC**, connecting out to the backend. It reaches any printer and balance, and would let tablets weigh and print through the backend. The cost is an installer, updates and code signing. Revisit it if printing from the browser or Web Serial proves too limited.
