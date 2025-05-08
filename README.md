> [!CAUTION]
> Due to my busy schedule, **the project is not currently in active development**.

![a creeper in a cave lit by lava](https://cdn.modrinth.com/data/cached_images/db98e8b5f28311e2c7edcd6e9cd00a82ba62f22b_0.webp)
The mod adds colored lighting to the game. Other mods that add colored lights can use it as a dependency.\
The mod is not compatible with Sodium!

# Features
- Different blocks can emit different colors
- Light that passes through stained glass is also colored
- Emitted colors, and filtered colors can be customized in resource packs
- The mod is client side - you can play with it on any server and you won't have any problems removing it from your world

# Resource Pack Tutorial
In your resourcepack's namespace folder (where folders like `textures` and `models` are located) create a `light` folder. There, you can create an `emitters.json` file, which defines what light colors blocks emit. Example:
\
_assets\\example\\light\\emitters.json_
```json
{
	"minecraft:torch": "#00FF00", // color in hex
	"minecraft:red_candle": "red", // dye name
	"minecraft:redstone_lamp": [ 0, 255, 255 ],
	"minecraft:soul_torch": "purple;10", // override light level emission
	"minecraft:oak_leaves": "light_blue;F" // value after ';' is a hex number from 0 to F
}
```
You can also create `filters.json`, where you define what light color passes through a given block. Example:\
_assets\\example\\light\\filters.json_
```json
{
	"minecraft:red_stained_glass": "#00FF00", // color in hex
	"minecraft:green_stained_glass": "red", // dye name
	"minecraft:glass": [ 0, 255, 255 ]
}
```

# Compatible Mods
Colorful Glowstone: (will be released soon)
https://modrinth.com/project/colorful-glowstone

# Compatible Resource Packs
Colorful Candles: [colorful-candles.zip](https://github.com/erykczy/colorful-lighting/raw/e372648afdd442e96340f0d8ee477d6ae8138739/addons/colorful-candles.zip)
