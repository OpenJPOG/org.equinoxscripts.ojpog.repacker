# Jurassic Park: Operation Genesis Model Repacker

A tool for Jurassic Park: Operation Genesis to repack the model files into the [LibGDX](https://github.com/libgdx/libgdx) .g3dj format.  It is almost feature complete, and is able to decode the entire `.tmd` file except for a few small values.

## Features
- Scene Graph
- Mesh
- Skinning Weights
- Animations

## TMD File Format, Overview
The TMD file format is decoded by the `TMD_*` files, so if you're interested in that look there.
- Scene Block
  - Nodes
  - Animations
    - Channel to Node Map
    - Channels
      - Key frames referring to the TKL database file.
- Mesh Block
  - Mesh Instances
      - Mesh Pieces
        - Bones
        - Vertices
        - Indices

## Building
This project uses the Maven build system, so a simple `mvn clean install` should create a binary.  Otherwise import it into the IDE of your choice.
One note is that the project does require the [LibGDX](https://github.com/libgdx/libgdx) natives, and while maven will acquire them you'll have to set the library path on your own.

## Pictures
[Visitor Center: Main Gates](https://puu.sh/sgFCD/d49fe2c3d9.gif)

[All Dinosaurs: Walking](https://puu.sh/sgyCK/9d246f29e7.gif)

[Acrocanthosaurus + Armature: Roar](https://puu.sh/sgiGO/ad0e86551b.gif)

[Dilophosaurus: Idle Sniff](https://puu.sh/sgFSW/b38be61b0e.gif)

## Credits
All textures and models used come from a combination of the [Forgotten Mod](http://www.moddb.com/mods/jpog-the-forgotten) and the original game.  Initial file format specification written and published by [Andres James](http://tresed.trescom.org/jpog/jpog_formats.html).

## Disclaimer
The information provided by this tool may not be used to break the law in any way, including but not limited to the use of the converted models 
in for profit projects and the redistribution of unpacked models.  I also make no assurances to the accuracy of this document; use at your own risk.
