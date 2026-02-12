/**
 * Converts plain text URLs to clickable HTML links
 * @param text - Plain text that may contain URLs
 * @returns HTML string with URLs converted to <a> tags
 */
export function linkify(text: string): string {
  // URL regex pattern that matches http(s) URLs
  const urlPattern = /(https?:\/\/[^\s<]+)/g

  // Replace URLs with HTML anchor tags
  return text.replace(urlPattern, (url) => {
    // Remove trailing punctuation that's not part of the URL
    let cleanUrl = url
    const trailingPunctuation = /[.,;:!?)\]}>]$/

    while (trailingPunctuation.test(cleanUrl)) {
      cleanUrl = cleanUrl.slice(0, -1)
    }

    return `<a href="${cleanUrl}" target="_blank" rel="noopener noreferrer" class="text-indigo-500 hover:text-indigo-400 underline break-all">${cleanUrl}</a>`
  })
}
