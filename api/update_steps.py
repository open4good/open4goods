import os

# 1. Update NudgeToolStepCategory.vue
path_cat = "../frontend/app/components/nudge-tool/NudgeToolStepCategory.vue"
abs_cat = os.path.abspath(path_cat)

try:
    with open(abs_cat, 'r') as f:
        content = f.read()

    # Increase gap in main container and add bottom margin/padding
    if ".nudge-step-category {\n  display: flex;\n  flex-direction: column;\n  gap: 12px;" in content:
        content = content.replace(
            ".nudge-step-category {\n  display: flex;\n  flex-direction: column;\n  gap: 12px;",
            ".nudge-step-category {\n  display: flex;\n  flex-direction: column;\n  gap: clamp(1.5rem, 4vw, 2.5rem);\n  padding-bottom: 1.5rem;"
        )
        print("Updated gap in NudgeToolStepCategory.vue")

    with open(abs_cat, 'w') as f:
        f.write(content)
except Exception as e:
    print(f"Error updating category step: {e}")

# 2. Update NudgeToolStepScores.vue
path_scores = "../frontend/app/components/nudge-tool/NudgeToolStepScores.vue"
abs_scores = os.path.abspath(path_scores)

try:
    with open(abs_scores, 'r') as f:
        content = f.read()

    # Change v-card to flat and add border
    # Finding the v-card definition
    # <v-card\n              v-bind="activatorProps"\n              class="nudge-step-scores__card nudge-option-card"
    
    target_card = 'class="nudge-step-scores__card nudge-option-card"'
    replacement_card = 'class="nudge-step-scores__card nudge-option-card" variant="flat" border="thin"'

    if target_card in content and 'variant="flat"' not in content:
        content = content.replace(target_card, replacement_card)
        print("Updated v-card to flat in NudgeToolStepScores.vue")

    # Increase grid gap
    # .nudge-step-scores__grid {\n    row-gap: 12px;\n  }
    if ".nudge-step-scores__grid {\n    row-gap: 12px;" in content:
        content = content.replace(
            ".nudge-step-scores__grid {\n    row-gap: 12px;",
            ".nudge-step-scores__grid {\n    row-gap: clamp(1rem, 2vw, 1.5rem);"
        )
        print("Updated grid gap in NudgeToolStepScores.vue")
        
    # Also update column spacing if possible (v-row dense might be too tight)
    # <v-row dense class="nudge-step-scores__grid">
    if '<v-row dense class="nudge-step-scores__grid">' in content:
         content = content.replace('<v-row dense class="nudge-step-scores__grid">', '<v-row class="nudge-step-scores__grid">')
         print("Removed dense prop from row in NudgeToolStepScores.vue")

    with open(abs_scores, 'w') as f:
        f.write(content)
except Exception as e:
    print(f"Error updating scores step: {e}")
