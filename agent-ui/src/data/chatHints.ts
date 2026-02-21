export interface ChatHint {
  label: string
  insertText: string
}

export const chatHints: ChatHint[] = [
  { label: 'Buy TOKEN', insertText: 'Buy ' },
  { label: 'Swap TOKEN', insertText: 'Swap ' },
  { label: 'Create order for TOKEN', insertText: 'Create order for ' },
  { label: 'Create price tracker for TOKEN', insertText: 'Create price tracker for ' },
]
