import os

file_path = "../frontend/app/components/nudge-tool/NudgeToolWizard.vue"
abs_path = os.path.abspath(file_path)

print(f"Updating {abs_path}")

try:
    with open(abs_path, 'r') as f:
        content = f.read()

    # 1. Inject minWindowHeight logic and update totalHeight
    target_logic = """const { height: headerHeight } = useElementSize(headerRef)
const { height: windowHeight } = useElementSize(windowWrapperRef)
const { height: footerHeight } = useElementSize(footerRef)

const totalHeight = computed(
  () => headerHeight.value + windowHeight.value + footerHeight.value + 32
) // 32 = padding"""

    replacement_logic = """const { height: headerHeight } = useElementSize(headerRef)
const { height: windowHeight } = useElementSize(windowWrapperRef)
const { height: footerHeight } = useElementSize(footerRef)

const minWindowHeight = ref(0)

watch(activeStepKey, (val) => {
  if (val === 'category') minWindowHeight.value = 0
})

watch(windowHeight, (val) => {
  if (activeStepKey.value !== 'category' && val > 0 && val > minWindowHeight.value) {
    minWindowHeight.value = val
  }
})

const totalHeight = computed(
  () => headerHeight.value + Math.max(windowHeight.value, minWindowHeight.value) + footerHeight.value + 32
) // 32 = padding"""

    # 2. Update Padding
    target_style = """padding: 16px;"""
    replacement_style = """padding: clamp(1.5rem, 3vw, 2rem);"""

    new_content = content
    if target_logic in new_content:
        new_content = new_content.replace(target_logic, replacement_logic)
        print("Logic updated.")
    else:
        print("Logic target not found.")

    # Only replace the first occurrence of padding: 16px which is in .nudge-wizard
    # Safe to assume it's the first one in the style block or usage?
    # Actually, simplistic replace might be dangerous if 'padding: 16px' is used elsewhere.
    # But looking at previous cat output, .nudge-wizard is the main container.
    # Let's try to be more specific if possible, or just replace all 16px padding to cleaner clamp?
    # There is also .nudge-step-category__card with padding: 16px 14px;
    # Let's target `.nudge-wizard {` context if possible or just rely on uniqueness.
    # The file content shows:
    # .nudge-wizard {
    #   position: relative;
    #   padding: 16px;
    
    if ".nudge-wizard {\n  position: relative;\n  padding: 16px;" in new_content:
         new_content = new_content.replace(".nudge-wizard {\n  position: relative;\n  padding: 16px;", ".nudge-wizard {\n  position: relative;\n  padding: clamp(1.5rem, 3vw, 2rem);")
         print("Style updated.")
    elif "padding: 16px;" in new_content:
         # Fallback but risky
         print("Specific style block not found, skipping generic replace to avoid side effects.")
    else:
         print("Style target not found.")

    with open(abs_path, 'w') as f:
        f.write(new_content)
    print("Successfully updated NudgeToolWizard.vue")

except Exception as e:
    print(f"Error: {e}")
