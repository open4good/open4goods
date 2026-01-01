const painImages = Object.values(
  import.meta.glob('~/assets/homepage/pain/*.{png,jpg,jpeg,webp,svg}', {
    eager: true,
    import: 'default',
  })
) as string[]

const gainImages = Object.values(
  import.meta.glob('~/assets/homepage/gain/*.{png,jpg,jpeg,webp,svg}', {
    eager: true,
    import: 'default',
  })
) as string[]

const pickRandomImage = (images: string[], fallback: string) => {
  if (!images.length) {
    return fallback
  }

  return images[Math.floor(Math.random() * images.length)]
}

export const useRandomHomepageImages = () => {
  const painImage = useState<string>('home-pain-random-image', () =>
    pickRandomImage(painImages, '/images/home/nudger-problem.webp')
  )
  const gainImage = useState<string>(
    'home-gain-random-image',
    () => '/images/home/nudger-problem.webp'
  )

  const pickGainImage = () => {
    // Pick a random image from gainImages, avoiding the current one if possible
    let newImage = gainImage.value
    if (gainImages.length > 1) {
      while (newImage === gainImage.value) {
        newImage = pickRandomImage(gainImages, gainImage.value)
      }
    } else {
      newImage = pickRandomImage(gainImages, gainImage.value)
    }
    gainImage.value = newImage
  }

  return {
    painImage,
    gainImage,
    pickGainImage,
  }
}
