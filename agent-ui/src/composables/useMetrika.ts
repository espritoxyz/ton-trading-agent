import { getCurrentInstance } from 'vue'

/**
 * Composable for easy access to Yandex Metrika functionality
 *
 * @example
 * ```typescript
 * const { reachGoal, userParams } = useMetrika()
 *
 * // Track a goal
 * reachGoal('PURCHASE', { amount: 100 })
 *
 * // Set user parameters
 * userParams({ UserID: '12345', Premium: true })
 * ```
 */
export function useMetrika() {
  const instance = getCurrentInstance()
  const metrika = instance?.appContext.config.globalProperties.$metrika

  return {
    /**
     * Track a page view
     * @param url - The URL to track
     */
    hit: (url: string) => {
      metrika?.hit(url)
    },

    /**
     * Reach a goal (conversion tracking)
     * @param target - The goal identifier (must be configured in Yandex Metrika)
     * @param params - Optional parameters to send with the goal
     */
    reachGoal: (target: string, params?: Record<string, any>) => {
      metrika?.reachGoal(target, params)
    },

    /**
     * Set user parameters
     * @param params - User parameters (e.g., UserID, Age, Gender)
     */
    userParams: (params: Record<string, any>) => {
      metrika?.userParams(params)
    },

    /**
     * Mark the session as not a bounce
     * Useful when you want to track engagement
     */
    notBounce: () => {
      metrika?.notBounce()
    },

    /**
     * Track external link click
     * @param url - External link URL
     */
    extLink: (url: string) => {
      metrika?.extLink(url)
    },

    /**
     * Track file download
     * @param url - File URL
     */
    file: (url: string) => {
      metrika?.file(url)
    },

    /**
     * Get the raw metrika instance for advanced usage
     */
    getInstance: () => metrika
  }
}
