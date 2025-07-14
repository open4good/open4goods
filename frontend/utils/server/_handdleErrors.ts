//Ici, créer une fonction qui va gérer les erreurs de façon plus globale
export function _handleError(error: any, message: string) {
  console.error(message, error)
  throw createError({
    statusCode: 500,
    statusMessage: message,
  })
}
