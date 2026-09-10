# Starfall Haven asset project

This is the first working version of the unified asset workflow. The owner has **deferred the statue and pedestal for now**, keeping the arch, gold eagle and banners. Further arch/eagle refinement remains invited. Their existing design is the baseline. The statue OBJ/MTL sources and earlier candidates are preserved, but excluded from the active manifest, exports and review. The planned central green stays open.

Use one entry point from the repository root:

```powershell
& 'client/starfall-haven/Build Assets.ps1' -Action Inspect
& 'client/starfall-haven/Build Assets.ps1' -Action Build
```

`Inspect` reads the project without creating a candidate. `Build` imports its editable source meshes, assembles the landmarks, exports OBJ/MTL + GLB + candidate native DAT, runs the actual client decoder, checks the arch passage samples, and creates one browser review with the approved design reference and editable banner artwork. The successful output is recorded in `build/starfall-content-project/latest.json`. Every build gets a fresh folder, source snapshot and receipt; previous candidates remain available. No client/cache/server release action is exposed.

The files you author are [project.json](project.json) and the OBJ/MTL assets in `source/`. Everything generated belongs under `build/starfall-content-project/`. The earlier procedural sources, previews and backups remain preserved under `models/v1` and their recorded build paths, but this builder **does not execute the procedural sculpt scripts or overwrite artist-authored sources**.

## Sculpting and enhancement workflow

Use Blender or another modelling tool that exports triangulated OBJ with solid material colours. [Blender offers sculpting](https://www.blender.org/features/sculpting/) and [OBJ/GLB interoperability](https://www.blender.org/features/pipeline/); it has not been installed or controlled by this batch.

1. **Arch:** keep the approved composition while improving stone profiles, recessed panels, vault geometry and the quality of the surface treatment. Maintain the broad passage and modular structure.
2. **Eagle:** improve anatomy and feather layering/shapes with modelling tools, then create a tested native-detail derivative. The current high-density feather study is not an established runtime budget.
3. **Banners and scenery:** continue to reuse native geometry and ordinary objects. Preserve custom artwork as editable masters.

The active manifest has six components, one assembled arch and four review views. Its reference is the existing arch/eagle design image. No replacement statue, statue sculpting or Meshy subscription is required for this scope.

Export coordinates must be X right, Y depth, Z up, with one horizontal unit equal to one tile. Keep each component's established origin (the pier, for example, is authored at X = −5.5). Materials use OBJ `mtllib` / MTL `Kd` colours. Triangulate before export. Texture maps are explicitly rejected by the current importer because the native texture conversion path has not been implemented; it will not silently throw texture data away.

## What is working and what comes next

Current arch-focused candidate: `candidate-9c3d8c79a86c4deb8e5075c8d831ca68`. Build/native decoding passed in 8.88 seconds for seven retained exports; focused importer/input and unchanged-export checks passed in 0.11 seconds; browser selection, visible geometry, rotation and wireframe passed for four views in 4.42 seconds. Statue/pedestal are absent from the new exports, snapshots, previews and pending-work list; all 16 source OBJ/MTL files remain byte-identical. Scope evidence, pre-edit backups and hashes: `build/starfall-content-project/statue-deferred-ed98e9376ccb44a3967a168f4411275d/`. No gameplay checks or publication were needed for this asset-selection change; native rendering, clipping and service integration remain pending.

The first build imported all eight components and two assemblies without changing their native DAT payloads. Exact vertices/faces/HSL match the existing client's decoder. It completed in 18.09 seconds. Focused importer/integration checks completed in 0.56 seconds, covering negative indices, material colours, malformed data, unsupported textures, path containment, input hashes and all ten baseline payloads. Evidence: the first candidate `candidate-54b3c86894634e56ac5463a40b8c16ce` under `build/starfall-content-project/`.

This prototype is a reusable **asset importer/build command and review viewer**, not a complete GUI editor or a new game client. It currently uses the existing isolated decoder class and local Java/Python paths; the wrapper accepts runtime/baseline overrides. A fresh machine needs those prerequisites provisioned. Arbitrary external meshes still need scale, materials, geometry and visual review.

Next content-tool stages are reference/asset browsing, model import controls and an isolated native preview, followed by cache model/object registration and map placement. Cache writing must first prove unchanged archive round-trips, metadata/checksums and delivery. Registered collision, native rendering/performance and all NPC/service actions still need their own implementation and acceptance. Reuse [the existing developer-client builder](../../DEVELOPMENT.md) for necessary client patches; this project does not justify a client rewrite.

Deferred research record, retained only if the owner later resumes the statue: the Smithsonian lists a [CC0 classical sculpture scan](https://3d.si.edu/object/3d/model-greek-slave%3A8edffe56-c358-4c3a-a61f-019f615ccef0). Its actual downloadable mesh could not be inspected (the collection page request returned HTTP 403), so it was neither imported nor selected. No further statue search or external generation is part of the active scope.
