# BiomeAtlas

A Forge mod for 1.7.10 that generates a map of biomes.

![Sample](readme/sample.png)

## Compiling

    ./gradlew clean build

## Usage

    /biomeatlas <apothem>

Or start a server with:

	-Dbiomeatlas.mapApothemAtStart=<apothem>

to have it map immediately after world load.
    
Apothem is the "radius" of a square around you in chunks (not blocks). Each chunk is a 16x16 area of blocks.

## License

MIT License.
