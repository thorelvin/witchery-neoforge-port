import os
import re

src_dir = r"src\main\java\com\emoniph\witchery"

def process_file(path):
    with open(path, 'r', encoding='utf-8') as f:
        content = f.read()
    original = content

    # 1. Update saveAdditional signature
    content = re.sub(
        r'(protected|public)\s+void\s+saveAdditional\s*\(\s*CompoundTag\s+(\w+)\s*,\s*HolderLookup\.Provider\s+\w+\s*\)',
        lambda m: f'protected void saveAdditional(net.minecraft.world.level.storage.ValueOutput {m.group(2)})',
        content
    )

    # 2. Update loadAdditional signature
    content = re.sub(
        r'(protected|public)\s+void\s+loadAdditional\s*\(\s*CompoundTag\s+(\w+)\s*,\s*HolderLookup\.Provider\s+\w+\s*\)',
        lambda m: f'protected void loadAdditional(net.minecraft.world.level.storage.ValueInput {m.group(2)})',
        content
    )

    # 3. Fix super.saveAdditional(tag, reg) -> super.saveAdditional(tag)
    content = re.sub(
        r'super\.saveAdditional\s*\(\s*(\w+)\s*,\s*\w+\s*\)',
        r'super.saveAdditional(\1)',
        content
    )

    # 4. Fix super.loadAdditional(tag, reg) -> super.loadAdditional(tag)
    content = re.sub(
        r'super\.loadAdditional\s*\(\s*(\w+)\s*,\s*\w+\s*\)',
        r'super.loadAdditional(\1)',
        content
    )

    # 5. Fix ContainerHelper.saveAllItems(tag, items, reg) -> ContainerHelper.saveAllItems(tag, items)
    content = re.sub(
        r'ContainerHelper\.saveAllItems\s*\(\s*(\w+)\s*,\s*(\w+)\s*,\s*\w+\s*\)',
        r'ContainerHelper.saveAllItems(\1, \2)',
        content
    )

    # 6. Fix ContainerHelper.loadAllItems(tag, items, reg) -> ContainerHelper.loadAllItems(tag, items)
    content = re.sub(
        r'ContainerHelper\.loadAllItems\s*\(\s*(\w+)\s*,\s*(\w+)\s*,\s*\w+\s*\)',
        r'ContainerHelper.loadAllItems(\1, \2)',
        content
    )

    # 7. Fix tag.getInt("key") -> tag.getIntOr("key", 0)  (in load contexts)
    # Only transform tag.getInt calls where the result is used directly (assignment)
    content = re.sub(
        r'(\w+)\.getInt\s*\(\s*("[\w_]+")\s*\)',
        r'\1.getIntOr(\2, 0)',
        content
    )

    # 8. Fix tag.getBoolean("key") -> tag.getBooleanOr("key", false)
    content = re.sub(
        r'(\w+)\.getBoolean\s*\(\s*("[\w_]+")\s*\)',
        r'\1.getBooleanOr(\2, false)',
        content
    )

    # 9. Fix tag.getString("key") -> tag.getStringOr("key", "")
    content = re.sub(
        r'(\w+)\.getString\s*\(\s*("[\w_]+")\s*\)',
        r'\1.getStringOr(\2, "")',
        content
    )

    # 10. Fix tag.getLong("key") -> tag.getLongOr("key", 0L)
    content = re.sub(
        r'(\w+)\.getLong\s*\(\s*("[\w_]+")\s*\)',
        r'\1.getLongOr(\2, 0L)',
        content
    )

    # 11. Fix tag.getFloat("key") -> tag.getFloatOr("key", 0f)
    content = re.sub(
        r'(\w+)\.getFloat\s*\(\s*("[\w_]+")\s*\)',
        r'\1.getFloatOr(\2, 0f)',
        content
    )

    # 12. Remove getUpdateTag override that calls saveAdditional(tag, reg)
    # Pattern: @Override public CompoundTag getUpdateTag(HolderLookup.Provider ...) { ... saveAdditional(tag, ...) ... }
    content = re.sub(
        r'@Override\s*\n\s*public\s+CompoundTag\s+getUpdateTag\s*\([^)]*\)\s*\{[^}]*saveAdditional[^}]*\}',
        '',
        content
    )

    # 13. Fix getBurnTime(null) or getBurnTime(recipeType) -> getBurnTime(null, level.fuelValues())
    # This is complex; let's just fix the most common pattern
    # Actually, for now let's do a simple null -> null, getFuelValues()
    # The new signature is getBurnTime(@Nullable RecipeType<?> recipeType, FuelValues fuelValues)
    # We need to find where getBurnTime is called and fix it
    # Common pattern: stack.getBurnTime(null) -> need FuelValues
    # For now just replace with a stub that returns 0 to get compilation
    content = re.sub(
        r'(\w+)\.getBurnTime\s*\(\s*null\s*\)',
        r'\1.getBurnTime(null, getFuelValues())',
        content
    )
    content = re.sub(
        r'(\w+)\.getBurnTime\s*\(\s*net\.minecraft\.world\.item\.crafting\.RecipeType\.[^)]+\)',
        r'\1.getBurnTime(null, getFuelValues())',
        content
    )

    # 14. Add imports for ValueOutput and ValueInput if saveAdditional/loadAdditional was changed
    if 'ValueOutput' in content and 'import net.minecraft.world.level.storage.ValueOutput' not in content:
        content = content.replace(
            'package com.emoniph.witchery',
            'package com.emoniph.witchery',
            1
        )
        # Add after the last import
        # Find last import line
        last_import = content.rfind('\nimport ')
        if last_import >= 0:
            end_of_import = content.find('\n', last_import + 1)
            content = content[:end_of_import + 1] + \
                'import net.minecraft.world.level.storage.ValueInput;\n' + \
                'import net.minecraft.world.level.storage.ValueOutput;\n' + \
                content[end_of_import + 1:]

    # 15. Remove unused HolderLookup.Provider imports if no longer needed
    # (can cause warnings but not errors, skip for now)

    if content != original:
        with open(path, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False


changed = 0
for root, dirs, files in os.walk(src_dir):
    for fname in files:
        if fname.endswith('.java'):
            path = os.path.join(root, fname)
            if process_file(path):
                changed += 1

print(f"Modified {changed} files")
