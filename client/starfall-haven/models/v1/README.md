# Starfall Haven: editable landmark models v1

Current scope: the owner has deferred the statue and pedestal and kept the arch, gold eagle and banners. This directory preserves earlier studies and its five-view review as historical evidence. Use the [active unified project](../../project/README.md) for the current build and four-view review; statue sources remain saved for later.

Actual editable meshes and renders are now available for review. The latest owner instruction is to retain as much detail from the approved design photo as possible. These files supersede the earlier design-only state; they do **not** represent an installed or live-accepted home.

Feedback on this iteration: **arch appearance approved; statue likeness rejected**. Face/chest and feather refinements were produced and are preserved, but the owner said the statue still did not resemble the original design. Do not equate successful decoding, increased mesh density or improved small details with artistic acceptance. The unified asset workflow was subsequently built and the statue later deferred; see the current project linked above.

Open [model-review.html](model-review.html) in a WebGL browser to turn, zoom and inspect the five asset views. It works offline and embeds the same geometry as the exports. Saved renders: [arch](previews/arch.png), [statue](previews/statue.png), [eagle](previews/eagle.png), [banners](previews/banners.png). These are actual triangle renders with studio lighting, not generated concept images or developer-client captures.

## What was made and reused

| Asset | Implementation |
| --- | --- |
| Arch | Custom modular pier, vaulted upper structure and central star plaque. One pier mesh is repeated on the other side. Includes raised pilasters, recessed panels, stone courses, cornice layers and masonry following the underside of the vault. |
| Gold eagle | Separate custom mesh with turned head, hooked beak, talons, tail, breast feathers and overlapping flight feathers. Surveyed native Eagle lever models 20036/20037/20038 were visually unsuitable for the outspread sculpture. |
| Statue | Custom continuous adult classical figure, topless with sculpted hair and drapery around the hips/legs. Low stepped plinth is separate. Revised after the owner rejected the level of detail: narrower shoulders, integrated face/body, swept hair and continuous hip folds. |
| Banners | Adapted **existing object 11675 / model 11557**, retaining its cloth and hanging-mount triangles, rotating/scaling it and recolouring locally. New eight-point star faces are added front and rear. No existing shared banner is overwritten. |
| Banner art | Editable SVG and clean transparent PNG: [arch banner](artwork/arch-banner.svg), [town standard](artwork/town-standard.svg). These are standalone artwork masters; the candidate native meshes use face colours and require no new texture/UV pipeline. |
| Other scenery and services | No new custom NPCs, shops, portals, observatory or ambient prop family. Ordinary scenery should still be reused. No gameplay behavior changes. |

The native banner extract and candidate survey are preserved in `build/starfall-models-v1/native/`. Its source SHA-256 is recorded in [mesh-manifest.json](mesh-manifest.json). Native source topology is preserved before quantization/welding and removal of degenerate faces; recolouring/rescaling and 32 emblem triangles produce 202 vertices / 215 triangles per final banner.

## Files and scale

Each of the eight components and two review assemblies has an OBJ + MTL, GLB and candidate native DAT in [exports](exports/). The `*.expected.json` files are decoder verification data. Authoring files are `build_assets.py`, `sculpt_statue.py`, `sculpt_eagle.py` and `meshlib.py`; `package_assets.py` validates and packages the results. `NativeAssetProbe.java` is an isolated read-only helper, not production server source.

- Authoring/OBJ axes: X horizontal, Y depth, Z up. One horizontal unit is one game tile. Native conversion: `(x, -z, y) × 128`, quantized to integers. GLB uses a right-handed rotation to Y up.
- Arch body is approximately 16 tiles wide and 6 deep, with cornice overhang; the opening is six tiles between the main piers. The upper assembly and mounted eagle reach about 12 tiles high. Exact bounds are in the manifest. These are geometry dimensions, not registered object footprints.
- Left pier is authored at X = −5.5; repeat it with X offset +11. Eagle assembly offset `(0, −0.1, 9.15)`, uniform scale 1.65. Front banner offsets `(±5.5, −2.90, 0.94)`.
- Statue plus plinth is about 3.875 units high; base diameter about 1.52 units. Native player-scale comparison is pending.
- The full arch and statue assemblies are review/reference exports. They are **not** instructions to register a solid rectangular arch object across the passage. Piers and vault must receive suitable separate placement/clipping treatment.
- After the requested face/chest and feather refinement, the source meshes are dense: statue figure 17,736 triangles, eagle 18,420 and full arch assembly 23,226. These are review sources, with native performance still unproven. Preserve them when making and testing optimized derivatives; increasing polygon counts further will not solve the statue's likeness.

## Visual comparison and remaining acceptance

The [approved home image](../../../../docs/project/assets/starfall-haven-final-concept.png) and [arch design sheet](../../design/v1/arch-eagle-design.png) remain the targets. First-pass and intermediate previews are retained under `build/starfall-models-v1/iterations/`.

The current assets reproduce the pale modular arch, gold outspread eagle, blue star banners and classical draped statue. The owner approved the arch's appearance, but explicitly rejected the statue's likeness after the additional refinement. The statue's face, anatomy and pose remain stylized; fine weathering and cloth weave are absent. Those differences must not be reported as photo-equivalent detail. A reference-based sculpt or a suitable licensed classical sculpture base is the proposed replacement for continued procedural approximation. The eagle has newly tapered vanes, raised shafts, diagonal detail and more separation; its final acceptance remains pending.

Detail revision: eyelids, brow/cheek contours, lips, nose and collarbone/chest relief are part of the actual statue mesh. [Statue close-up](previews/statue-detail.png) shows the selected exported upper-body triangles at larger scale; its cut lower edge is a review crop of geometry, not damage to the complete statue. Studio/GLB smooth normals now use corner-angle weighting; native lighting remains a separate acceptance boundary. The previous complete v1 sources/exports/renders and document copies are preserved in `build/starfall-models-v1/iterations/detail-request-2/`.

No textures, shaders, model/object IDs, cache records, map placements, click actions or collision definitions have been installed. GLB inspection is double-sided; this does not establish native backface/culling correctness. Native HSL colour quantization and in-game illumination can differ from the RGB studio renders. Test the exact candidate in an isolated developer-client scene before calling it game-ready; include materials, camera views, surface visibility, performance, scale, arch passage/side routes and statue approaches. Further model optimization must preserve the approved silhouette and detail sources.

All advertised NPC dialogues, banks, shops, travel, repairs and rewards remain required by [the home plan](../../../../docs/project/STARFALL_HAVEN_PLAN.md). Asset decoding does not test these services or authorize migration.

## Validation and reproduction

Selected checks address the asset contracts only. [validation.json](validation.json) records exact candidate hashes and results:

1. The original client JAR's `Class178(byte[])` decoded all ten DAT exports. Vertices, cyclic face winding and HSL face colours match the expected records exactly.
2. OBJ coordinates, winding and palette match those same candidates; triangles have finite coordinates, valid indices and nonzero area. GLB files carry the same geometry with the stated axis rotation. The offline viewer's embedded geometry matches the exports.
3. Twenty depthwise ray samples through the central passage, across five widths and four heights up to 2.75 units, intersect no arch triangles. Control rays intersect both piers. This is sampled geometry evidence, **not server clipping or movement acceptance**.
4. Banner PNGs have transparent backgrounds. SVG sources are provided separately from native face-colour art.
5. A headless Edge WebGL smoke check displayed all five assets, confirmed visible geometry, exercised selection, orbit and wireframe, and reported no page errors. Log: `build/starfall-models-v1/viewer-validation.json`.

Measured detail-revision model build: 42.48 seconds; exact export/package checks: 3.21 seconds; browser smoke: 4.62 seconds. Native decoder duration was not separately instrumented. Logs are `detail-2-build.log`, `detail-2-decoder.log`, `detail-2-validation.log` and `detail-2-viewer.log` under `build/starfall-models-v1/`. The package check also independently loads each GLB and verifies that the approved arch components, banners, base and artwork remain byte-identical to the saved pre-revision files. There was no gameplay suite, full server compilation, staging, publishing or restart.

Run from the repository root with the bundled Python. `build_assets.py` uses the workspace-local NumPy/scikit-image/fast-simplification installation in `build/starfall-models-v1/python-packages`; its pinned versions are in `build/starfall-models-v1/model-libraries-install.log`. Windows sandbox access to those installed libraries and the original client JAR required elevated tool execution. No system Python installation was modified.

```powershell
& 'C:/Users/Tcarn/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' client/starfall-haven/models/v1/build_assets.py
& 'C:/Program Files/Java/jre1.8.0_503/bin/java.exe' -cp "build/starfall-models-v1/classes;bin;lib/netty.jar;C:/Users/Tcarn/Desktop/DyNamic's 639/DyNamic-local.jar" NativeAssetProbe decode build/starfall-models-v1/decoded client/starfall-haven/models/v1/exports
& 'C:/Users/Tcarn/.cache/codex-runtimes/codex-primary-runtime/dependencies/python/python.exe' client/starfall-haven/models/v1/package_assets.py
& 'C:/Users/Tcarn/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node.exe' build/starfall-models-v1/viewer-smoke.cjs
```

The helper's compiled class is isolated in `build/starfall-models-v1/classes`. Recompile only that helper when needed, using the existing JAR and server cache classes, `--release 8`, and an empty `-sourcepath`; do not compile or stage a server batch for an asset-only rebuild. Preserve the native survey and original references.

Evidence state: **editable assets built; scoped automated checks passed; arch appearance approved, statue likeness rejected, refined eagle awaiting acceptance; native rendering/performance and collision pending; staged no; loaded no; live accepted no.** Document backups and before/after SHA-256 manifests are under `build/starfall-models-v1/`. No production source, runtime, cache, launcher, account or journal changes were part of this work.
