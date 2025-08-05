//Ici, créer une fonction qui va gérer les erreurs de façon plus globale
export function _handleError(error: unknown, message: string): never {
  console.error(message, error)
  throw createError({
    statusCode: 500,
    statusMessage: message,
  })
}
