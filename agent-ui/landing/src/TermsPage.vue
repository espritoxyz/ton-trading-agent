<script setup>
// Generate random stars
const stars = Array.from({ length: 150 }, () => ({
  left: `${Math.random() * 100}%`,
  top: `${Math.random() * 100}%`,
  animationDelay: `${Math.random() * 3}s`,
  size: Math.random() > 0.7 ? '3px' : '2px'
}))
</script>

<template>
  <div class="relative min-h-screen overflow-hidden">
    <!-- Animated Background -->
    <div class="stars">
      <div
        v-for="(star, i) in stars"
        :key="i"
        class="star"
        :style="{ left: star.left, top: star.top, animationDelay: star.animationDelay, width: star.size, height: star.size }"
      />
    </div>

    <!-- Gradient Orbs -->
    <div class="orb w-96 h-96 bg-cosmic-500 top-0 right-0" />
    <div class="orb w-80 h-80 bg-purple-600 bottom-20 left-10" style="animation-delay: 5s" />

    <!-- Main Content -->
    <div class="relative z-10">
      <!-- Navigation -->
      <nav class="fixed top-0 w-full glass-card border-b border-white/5 backdrop-blur-xl z-50">
        <div class="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
          <div class="flex justify-between items-center h-20">
            <a href="/" class="flex items-center gap-3">
              <svg width="40" height="40" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
                <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
                <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
              </svg>
              <span class="text-2xl font-bold gradient-text">Esprito App</span>
            </a>
            <div class="hidden md:flex items-center gap-8">
              <a href="/" class="text-gray-300 hover:text-white transition">Home</a>
              <a href="/blog" class="text-gray-300 hover:text-white transition">Blog</a>
              <a href="/app" class="cta-button px-6 py-2 rounded-full font-semibold">
                Launch App
              </a>
            </div>
          </div>
        </div>
      </nav>

      <!-- Header -->
      <section class="pt-32 pb-12 px-4 sm:px-6 lg:px-8">
        <div class="max-w-4xl mx-auto text-center">
          <h1 class="text-4xl md:text-5xl font-bold mb-4">
            Terms of <span class="gradient-text">Service</span>
          </h1>
          <p class="text-gray-400">Last updated: January 2026</p>
        </div>
      </section>

      <!-- Content -->
      <section class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-8 md:p-12 prose prose-invert">

            <!-- Section 1 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">1. General Terms</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface at esprito.app provides access to the Esprito App protocol (an AI-powered trading assistant and automated market making system). Users accessing this interface agree to be bound by these Terms of Use and Privacy Policy.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Key representations users must make:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Have reached age of majority</li>
              <li>Are not located in prohibited jurisdictions</li>
              <li>Are not subject to economic sanctions</li>
              <li>Will not facilitate illegal activity</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">2. Interface, Protocol and Underlying Smart Contracts</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Protocol operates via self-executing code on the TON blockchain. The Interface does not control the Smart Contracts—it only enables interaction with them.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4 italic">
              "When you initiate an action (such as adding or removing liquidity) the Interface generates an unsigned transaction draft"
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">3. Eligibility</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users represent and warrant they:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Possess legal capacity to agree to Terms</li>
              <li>Have technical knowledge of associated risks</li>
              <li>Will comply with all applicable laws</li>
              <li>Are not "Prohibited Persons" (sanctioned individuals/entities)</li>
              <li>Will not circumvent access restrictions</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">4. Permitted Use</h2>
            <p class="text-gray-300 leading-relaxed mb-4 italic">
              "The Interface is provided 'as is' and solely as an informational aid to technologically sophisticated users"
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users must independently verify all information before relying on it. The Interface provides no recommendations or professional advice.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">5. Third-Party Services and Content</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface may link to or embed third-party resources (wallets, protocols, block explorers). These have separate terms and conditions. Users are solely liable for costs associated with third-party resource use.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">6. Prohibited Use</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users cannot:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Use the Interface for unauthorized purposes</li>
              <li>Employ fraud or deception</li>
              <li>Attack or compromise security systems</li>
              <li>Violate applicable laws</li>
              <li>Facilitate securities offerings that violate regulations</li>
              <li>Engage in market manipulation</li>
              <li>Circumvent sanctions or trade restrictions</li>
              <li>Infringe intellectual property rights</li>
              <li>Transmit proceeds from criminal activity</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">7. Warranties and Representations</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface makes no warranties regarding:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Continuous or error-free operation</li>
              <li>Accuracy of information</li>
              <li>Third-party wallet performance</li>
              <li>Professional advice</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4 italic">
              "Users acknowledge the inherent risks of using decentralized protocols and accept full responsibility for any resulting losses."
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">8. Limitation of Liability</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              To the fullest extent permitted by law, in no event shall the Interface or its affiliates be liable for:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Any indirect, incidental, special, consequential or punitive damages</li>
              <li>Loss of profits, data, use, goodwill, or other intangible losses</li>
              <li>Any damages arising from use or inability to use the Interface</li>
              <li>Any damages resulting from unauthorized access to or use of our servers and/or any personal information stored therein</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              "Some jurisdictions do not allow the exclusion or limitation of incidental or consequential damages, so the above limitations may not apply to you."
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">9. Indemnification</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users agree to indemnify, defend and hold harmless the Interface and its affiliates from any claims, damages, losses, liabilities, costs or expenses (including reasonable attorneys' fees) arising out of or in any way connected with:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Your access to or use of the Interface</li>
              <li>Your violation of these Terms</li>
              <li>Your violation of any rights of another party, including any intellectual property rights</li>
              <li>Your violation of any applicable laws or regulations</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">10. Governing Law and Dispute Resolution</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              These Terms shall be governed by and construed in accordance with the laws of the jurisdiction in which the Interface is operated, without regard to its conflict of law principles.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Any disputes arising out of or in connection with these Terms shall be resolved through binding arbitration in accordance with the rules of the applicable arbitration association. The place of arbitration shall be the jurisdiction in which the Interface is operated.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">11. Changes to Terms</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface reserves the right to modify these Terms at any time. Users will be notified of any material changes via the Interface or by other means. Continued use of the Interface after such changes constitutes acceptance of the new Terms.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">12. Miscellaneous</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              These Terms constitute the entire agreement between the user and the Interface regarding the subject matter hereof and supersede all prior agreements and understandings, whether written or oral, relating to such subject matter.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              If any provision of these Terms is found to be invalid or unenforceable, the remaining provisions shall continue in full force and effect.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              The failure of the Interface to enforce any right or provision of these Terms shall not be deemed a waiver of such right or provision.
            </p>
          </div>
        </div>
      </section>

      <!-- Footer -->
      <footer class="py-12 px-4 sm:px-6 lg:px-8 border-t border-white/10">
        <div class="max-w-7xl mx-auto">
          <div class="grid md:grid-cols-4 gap-8 mb-12">
            <div class="col-span-2 md:col-span-1">
              <div class="flex items-center gap-3 mb-4">
                <svg width="32" height="32" viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M0 38.4C0 24.9587 0 18.2381 2.61584 13.1042C4.9168 8.58834 8.58834 4.9168 13.1042 2.61584C18.2381 0 24.9587 0 38.4 0H57.6C71.0413 0 77.7619 0 82.8958 2.61584C87.4117 4.9168 91.0832 8.58834 93.3842 13.1042C96 18.2381 96 24.9587 96 38.4V57.6C96 71.0413 96 77.7619 93.3842 82.8958C91.0832 87.4117 87.4117 91.0832 82.8958 93.3842C77.7619 96 71.0413 96 57.6 96H38.4C24.9587 96 18.2381 96 13.1042 93.3842C8.58834 91.0832 4.9168 87.4117 2.61584 82.8958C0 77.7619 0 71.0413 0 57.6V38.4Z" fill="#6366f1"/>
                  <path d="M71.6404 45H24.3596C20.9765 45 19.3231 40.8734 21.7703 38.5375L45.4107 15.9716C46.8598 14.5884 49.1402 14.5884 50.5893 15.9716L74.2297 38.5374C76.6768 40.8734 75.0235 45 71.6404 45Z" fill="white"/>
                  <path fill-rule="evenodd" clip-rule="evenodd" d="M28.1159 52.8038C27.4167 52.1278 26.4821 51.75 25.5096 51.75L24.3595 51.75C20.9764 51.75 19.323 55.8766 21.7702 58.2126L45.4106 80.7784C46.8597 82.1616 49.1401 82.1616 50.5891 80.7784L74.2295 58.2126C76.6767 55.8766 75.0233 51.75 71.6403 51.75L70.4901 51.75C69.5175 51.75 68.583 52.1278 67.8837 52.8038L50.6062 69.5055C49.1526 70.9105 46.847 70.9105 45.3935 69.5054L28.1159 52.8038Z" fill="white"/>
                </svg>
                <span class="text-xl font-bold">Esprito App</span>
              </div>
              <p class="text-gray-400 text-sm">Your intelligent companion for trading on the TON blockchain.</p>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Product</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/#features" class="hover:text-white transition">Features</a></li>
                <li><a href="/roadmap" class="hover:text-white transition">Roadmap</a></li>
                <li><a href="/app" class="hover:text-white transition">Launch App</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Company</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/blog" class="hover:text-white transition">Blog</a></li>
                <li><a href="#" class="hover:text-white transition">Contact</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Legal</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/terms" class="hover:text-white transition">Terms of Service</a></li>
                <li><a href="/privacy" class="hover:text-white transition">Privacy Policy</a></li>
              </ul>
            </div>
          </div>

          <div class="pt-8 border-t border-white/10 flex flex-col md:flex-row justify-between items-center gap-4">
            <p class="text-gray-400 text-sm">
              © 2026 Esprito Tech QFZ LLC. All rights reserved.
            </p>
            <div class="flex items-center gap-6 text-gray-400 text-sm">
              <span>Built with ❤️ for the TON ecosystem</span>
            </div>
          </div>
        </div>
      </footer>
    </div>
  </div>
</template>

<style scoped>
.cta-button {
  background: linear-gradient(90deg, #6366f1 0%, #a855f7 50%, #6366f1 100%);
  background-size: 200% 100%;
  animation: gradientFlow 3s ease infinite;
  box-shadow: 0 0 30px rgba(99, 102, 241, 0.4);
  transition: all 0.3s ease;
}

.cta-button:hover {
  box-shadow: 0 0 50px rgba(99, 102, 241, 0.6);
  transform: translateY(-2px);
}
</style>
