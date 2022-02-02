# Bedrock Block Palettes

Starting with v411, the block palette is no longer sent as a parameter in the StartGamePacket. 

After obtaining, place the block palette here. You can use this Bedrock server mod to generate a block palette: https://github.com/pmmp/mapping

# Bedrock Item Palettes

mappings-generator uses the item palette dumped from Minecraft Bedrock using [ProxyPass](https://github.com/NukkitX/ProxyPass).

The required palette is included in this repository, however if you would like to dump your own, follow these steps:

After running ProxyPass and connecting to it from Minecraft Bedrock the palettes should be dumped to the data folder. 

Currently, mappings-generator only uses `runtime_item_states.json`; copy that file to this directory.

# Bedrock Biome Mappings

Biome mappings are obtained from https://github.com/pmmp/BedrockData with thanks to the PocketMine team.
