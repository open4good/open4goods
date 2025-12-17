import os

file_path = "../frontend/app/components/home/sections/HomeHeroSection.vue"
abs_path = os.path.abspath(file_path)

print(f"Updating {abs_path}")

try:
    with open(abs_path, 'r') as f:
        content = f.read()

    target = """.home-hero__panel
  backdrop-filter: blur(10px)
  background: rgba(var(--v-theme-surface-glass), 0.82)
  border-radius: clamp(1.5rem, 4vw, 2rem)
  box-shadow: 0 18px 40px rgba(var(--v-theme-shadow-primary-600), 0.16)
  padding: clamp(1.5rem, 4vw, 2.5rem)

.home-hero__panel-grid
  display: grid
  gap: clamp(1.25rem, 3vw, 1.75rem)
  grid-template-columns: 1fr"""

    replacement = """.home-hero__panel
  background: rgb(var(--v-theme-surface-default))
  border-radius: clamp(1.5rem, 4vw, 2rem)
  box-shadow: 0 4px 12px rgba(var(--v-theme-shadow-primary-600), 0.05)
  padding: clamp(2rem, 5vw, 3rem)
  margin-block-start: 1rem

.home-hero__panel-grid
  display: grid
  gap: clamp(1.5rem, 4vw, 2.5rem)
  grid-template-columns: 1fr"""

    if target in content:
        new_content = content.replace(target, replacement)
        with open(abs_path, 'w') as f:
            f.write(new_content)
        print("Successfully updated HomeHeroSection.vue")
    else:
        print("Target string not found!")

except Exception as e:
    print(f"Error: {e}")
