# Checkpoint: mobile fusion R0 inventory

Date: 2026-10-07. Scope remains R0–R2 offline only. Raw private artifacts remain on Drive and are not committed.

## Exporter source and schema

- `HS_Post_Match_Exporter_Source_v0.4_2026-10-07.zip`: 534157 bytes, SHA-256 `de1cee05e9df28a98ab4a0755d8d5a56995c11fb943ad922e3c8e9358c05f153`.
- Declared app identity: `com.sox.hslogdiag`, versionCode 6, versionName 0.4, Java 17, compile/target SDK 35, Shizuku 13.1.5.
- Bundle schema: `hs-export-bundle/0.4`; per-match schema observed in the representative bundle: `hs-power-evidence/0.3`.
- `card-catalog.json`: 514 bytes, SHA-256 `4ccc4e23d4033cc94c5c785b9a26e037b5551cf986c3d7ad9821b1ec13e82a1b`.
- `card-names.bin`: 497614 bytes, SHA-256 `1b7558052e837c405882fc3a100472d1b0b4526b29563d4ffd207778868792a7`.
- `PowerParser.java`: 10594 bytes, SHA-256 `03184039981b2bc138ec3a404bad35a6326c8cc603209b3efd739870fbf7368d`.

The v0.4 parser selects GameState when present, otherwise PowerTaskList; preserves source line/time/raw records; segments on CREATE_GAME; records FINAL_GAMEOVER and truncation; caps matches at 100000 selected events; and keeps offered, submitted and confirmed choice evidence separate. Its raw event JSON is input evidence, not the fused output schema.

## Private fixture inventory

Exporter bundles, filename to SHA-256:

- `HS-export-20261007-110747-2272cd33.zip`: `6f0f7e8d65f242cb7b57b45d1dec0e9423c89a09fc28dc44dae0c686a2749e10`
- `HS-export-20261007-110749-cf440038.zip`: `3f5818836410ff3a63a7284962dd81425f668629bac095691461e594d68b2503`
- `HS-export-20261007-111036-b7a67ce3.zip`: `7dd993a47d623f9654b25b331c5ece2815654003dcc47c9ea0e32878bf96363b`
- `HS-export-20261007-113129-d6fcdf3a.zip`: `a14a4ce49d54cabb9f93ceb4a9abb9acca867916c45b93faef4d8a77621b8552`
- `HS-export-20261007-114457-2d4a92bd.zip`: `5b68bded1a2ae1ca5bc045dc5ccc0a43095378dc3e990e813cd1041dfee9e6db`
- `HS-export-20261007-120905-b91f0ce1.zip`: `9c731e4ce8c09a710d971cb0fa5b13dccee8cf12dc7c1fc6bfaec5e4e14fd71f`
- `HS-export-20261007-135643-ba507a42.zip`: `525adb245802a072e0da5bd88ae59cd85bc9b7a0fd278c21e565c13f8257921a`
- `HS-export-20261007-142822-44f30f8a.zip`: `6e89dacbe05f040a10ab5a73ac79e2edc31934d2c3e3fbf18d94a981ff038d4e`
- `HS-export-20261007-174816-10dc47e2.zip`: `49c306cd1f68fc6cba60a7d52e62116d52081d340e1606c8c71ee07655748aac`
- `HS-export-20261007-174833-2fd24e42.zip`: `4f947182a79542a662709ba484819957a065dc4b65d57c2f20e8c09496354da8`
- `HS-export-20261007-183243-2da66cfb.zip`: `ba003ad570ad9f672a9b9984db7581ade4da76846df26a12299ccb80b49954f4`

There are 19 tracker `match_*.json` fixtures. The independently inspected representative pair is:

Tracker fixture SHA-256 inventory:

- `match_20261006T010602189Z_170c29ebf6a1ac9810decb7d06d5889b5bd7826566b9f6b337ad222ef900123d.json`: `7133609191cfbd2eee5b585af8385b3767d64359f0b1eb9126cdf18dadcf3193`
- `match_20261006T033605754Z_8a02e0efe1b31a6c7490fea3132a0273f0001d74f6544c3d273b365c5453d9c0.json`: `c820373c1f820e721b0014210606a2c02df36f4a1c049807c8042e9234e40114`
- `match_20261006T041437900Z_b5ed9842ae75e91280dcb61783f13d03cc9aec1301983f29db0e4ea6b755a852.json`: `f48217845237d7f248f07a5a853394d78519359d13c043e0fd4fa498009a5898`
- `match_20261006T043702883Z_5cca0df957f1a390b1961d9def574c4053daf1cb6d133ab08cbd084123aa896d.json`: `84224a5208671cc62792e48692367a2555cef39a7a8ba3d81718ebf1595d5328`
- `match_20261006T181137976Z_1a6f055fc130c3a1059b3fd2fc95a1daa8da56e907a98a20389874d759473a70.json`: `efc9a16bf5e2c87dab884ac09af835ffc75db088a34dff1a5fc0fbb7686073b0`
- `match_20261006T182817758Z_68796488b30b5a4b0978a2e60e9ee27740b4bced5589e2abb24625f32566ad89.json`: `fdcefff38debf28afa21bbf53962870d1b8d8e12a32d5cbe2f939743ef6068b6`
- `match_20261006T200436174Z_fcddd38bfcdab1b7eefe7e23a3202029abd743542924dda619ce6aa8fca4619a.json`: `3de76453ed7172006fd43a436d159e6f57334cb1ab5c090b48eab4c2c1fd51da`
- `match_20261007T000354423Z_19209fe55c0163c39c5446ec9b183c40f5959b13aaf71c98e42e1d291267cbe9.json`: `715e944b18f57cf005e3565a5454be24ab19ce08f1759515a2afcd67e87bf1e4`
- `match_20261007T002453746Z_ad8a7bc6510136ac4aab8737d3d9cbc54b518bcc19a9cf83ab9fcd40b8cfa1d3.json`: `9d79f7cc5cab632c41daf59d13f9f556791e0c1d1109ff5f10900e37ea706574`
- `match_20261007T044712279Z_d4f7c07b22287489a2e259104cdaf778b6686ef0e2e31ccd51d1408d7900b3a9.json`: `d0e2c4eb52b2305ccfe11c5de555e2a871e067e4e99ff62860d32d296d84ed86`
- `match_20261007T051338743Z_08fc47e156e2d41663cc4d76b732b85d5260971599c7c2e8255f0dafd48fc788.json`: `457153853a80b4c14efed2dc4a301aca8fd86bcb55ad241db9653f598599d0c0`
- `match_20261007T054630690Z_54f494a9957616abe1f6ab1b6807bc2d553fab95204ca1743dbe50470770021b.json`: `785b455dedeeb2bed273d5d73c575b71434b2306926bf2a61a858778f8e972ee`
- `match_20261007T063917393Z_fb638c67673fe9f04755c64d2fe9493c3d5ae27cdb118960b416caca68440b72.json`: `fd86a704518d1bbcac2a5564b7760c84f34cfd968e6a047a97ef4db72823fbe6`
- `match_20261007T065558653Z_979539251cb23479b3bf3c06b9ad540ad6dcbc9d263f0950e591b5a18fdd22c7.json`: `eab6447658c64a17867ead81963b13a74cb3893524ae3aac057fb0d1056fc718`
- `match_20261007T073619776Z_7a2bf748fe700d2a3e2278ae4e9c6ed423b1f0f05af792e8190fd45eb60c9462.json`: `f106c1a03dbd81cc3ef955ee3e60c8a47900ee7ade12fae64c479c9d130778d0`
- `match_20261007T154348064Z_944cf25b5fb9cb700e2122b6e1eca82f4b7923e6dae1fbe37b25b4e49b934b15.json`: `9e314581ee4fccc40cc79be4817ee9854ba9c12b6ba6c363c4b75854b362075e`
- `match_20261007T170706547Z_e5732e521d4bd2445cd204542fc4783459659087cba9da5ab8f453ccee6f2514.json`: `063274ea05271dc85c710cfef146bd3b226acc97b89ce4112018f420847848bd`
- `match_20261007T192626774Z_961f10263d57920408090f2fbccc7518a75e84983274cb123f0b5f41a4bf7cb0.json`: `d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574`
- `match_20261007T224522751Z_29f123dad3c94ddce12192af0bc09665e0238c0a42de12dc3a76fe0749a67367.json`: `dc9e19397bce99fbf19beb8793ef70d6ec25ddb55e31fba6e243004306ecbdca`

- Tracker `match_20261007T192626774Z_961f10263d57920408090f2fbccc7518a75e84983274cb123f0b5f41a4bf7cb0.json`: 7840 bytes, SHA-256 `d9b037917de5007d456b305850d3dd9e6ddaf919d59b8e2928d08de31057d574`.
- Exporter `HS-export-20261007-142822-44f30f8a.zip`: hash above; inner Power.log manifest hash `2c05f4bf27aca34e440ef4fc3a554759ffe40605f01beaa9bdf3193b52b38744`.
- Independently observed expectations: tracker WIN, 18 reported turns and 73 timeline entries; exporter match-001 completed with nine choices; the source ends about seven seconds before the tracker timestamp. These facts support tests but do not make timestamp equality a pairing rule.

## Installed-build uncertainty

`app-debug.apk` is 68638838 bytes with SHA-256 `a1a74f5ca74b10adaee1a735da719182a81e32f91da4e5124b1320ac3a06e055`. Its presence and compatible PLAYER_PLAY output do not prove which exact source commit produced the APK, which signing identity is installed on the phone, or whether that APK is currently installed. The branch preserves the four tracker patches, but branch identity must not be substituted for installed-build identity.

## Acceptance boundary

R0 source/artifact availability and inventory are complete. The plan's narrative was cross-checked against raw source and the representative pair. Full real-fixture R1/R2 acceptance remains open and must not be inferred from this inventory or from synthetic tests.
