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
              <a href="/blog.html" class="text-gray-300 hover:text-white transition">Blog</a>
              <a href="http://localhost:5173" target="_blank" class="cta-button px-6 py-2 rounded-full font-semibold">
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
            <p class="text-gray-300 leading-relaxed mb-4">
              Use of VPNs to circumvent access restrictions is prohibited.
            </p>

            <!-- Section 2 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">2. Interface, Protocol and Underlying Smart Contracts</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Protocol operates via self-executing code on the TON blockchain. The Interface does not control the Smart Contracts—it only enables interaction with them.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4 italic">
              "When you initiate an action (such as adding or removing liquidity) the Interface generates an unsigned transaction draft"
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users retain full responsibility for:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Holding private keys</li>
              <li>Reviewing transaction details before signing</li>
              <li>Broadcasting transactions via their wallet</li>
              <li>Monitoring confirmation and paying gas fees</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface aggregates publicly available blockchain data for convenience only.
            </p>

            <!-- Section 3 -->
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

            <!-- Section 4 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">4. Permitted Use</h2>
            <p class="text-gray-300 leading-relaxed mb-4 italic">
              "The Interface is provided 'as is' and solely as an informational aid to technologically sophisticated users"
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users must independently verify all information before relying on it. The Interface provides no recommendations or professional advice.
            </p>

            <!-- Section 5 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">5. Third-Party Services and Content</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface may link to or embed third-party resources (wallets, protocols, block explorers). These have separate terms and conditions. Users are solely liable for costs associated with third-party resource use.
            </p>

            <!-- Section 6 -->
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

            <!-- Section 7 -->
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
              "You will not hold the Interface or its maintainers liable for any losses or damages arising from your use"
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users acknowledge no fiduciary duty exists.
            </p>

            <!-- Section 8 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">8. Risk Disclaimers</h2>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(a) No Business Plan or Support Guarantee</h3>
            <p class="text-gray-300 leading-relaxed mb-4 italic">
              "The Interface is maintained as a free, open-source application by the maintainers and contributors. There is no revenue model or long-term funding commitment."
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Support may be reduced or discontinued without notice.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(b) Incomplete or Outdated Information</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Data may be incomplete or removed at maintainers' discretion.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(c) Multiple Protocol Deployments</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Third parties may fork or relicense the Protocol. Maintainers have no obligation to support alternatives.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(d) No Regulatory Oversight</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface is not regulated or supervised by any government authority.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(e) Regulatory and Legal Uncertainty</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Evolving legal landscapes may impact ability to use the Interface or hold digital assets.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(f) Third-Party Content</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Links to third-party resources carry no endorsement or warranty.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(g) Cryptography and Technological Risks</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Quantum computing or cryptanalysis advances may compromise security systems.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(h) Forks and Network Changes</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Blockchain networks may fork or upgrade. Maintainers cannot predict or guarantee support for forked networks.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(i) Essential Third-Party Dependencies</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Interface relies on third-party infrastructure with no control or liability guarantees.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">8(j) Taxes</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users bear sole responsibility for tax compliance related to Protocol transactions.
            </p>

            <!-- Section 9 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">9. No Warranties and Representations</h2>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase font-semibold">
              THE INTERFACE IS PROVIDED ON AN "AS IS" AND "AS AVAILABLE" BASIS WITHOUT WARRANTIES
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              All warranties of merchantability, fitness for purpose, title, non-infringement, availability, accuracy, and security are expressly disclaimed.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              No guarantee of continuous access, accurate information, or virus-free operation.
            </p>

            <!-- Section 10 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">10. Release and Indemnification</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users agree to defend and indemnify Interface maintainers and associated parties from claims, damages, and losses arising from:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>User access and use of the Interface</li>
              <li>Violations of Terms or applicable law</li>
              <li>Third-party use with user assistance</li>
              <li>Disputes with other users</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users waive all liability claims against maintainers.
            </p>

            <!-- Section 11 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">11. Limitation of Liability</h2>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase font-semibold">
              UNDER NO CIRCUMSTANCES SHALL ANY OF INDEMNITY PARTIES BE LIABLE TO YOU FOR ANY INDIRECT, PUNITIVE, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR EXEMPLARY DAMAGES
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Maintainers assume no liability for errors, personal injury, property damage, unauthorized access, interruptions, bugs, viruses, or defamatory content.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Maximum liability capped at $100 USD across all damages (where not prohibited by law).
            </p>

            <!-- Section 12 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">12. Governing Law</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Terms governed by British Virgin Islands law, excluding conflict of laws principles.
            </p>

            <!-- Section 13 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">13. Arbitration</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Disputes require 100 days of good-faith negotiation via email. Unresolved disputes proceed to binding arbitration under British Virgin Islands Arbitration Act (2013).
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Single or three arbitrators</li>
              <li>Seat in British Virgin Islands</li>
              <li>Conducted in English</li>
              <li>Confidential proceedings</li>
            </ul>

            <!-- Section 14 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">14. Class Action and Jury Trial Waiver</h2>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase font-semibold">
              YOU AGREE TO BRING ALL DISPUTES IN YOUR INDIVIDUAL CAPACITY AND NOT AS A PLAINTIFF IN OR MEMBER OF ANY CLASS ACTION
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Users waive right to jury trial.
            </p>

            <!-- Section 15 -->
            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">15. General Provisions</h2>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">(a) Assignment</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Terms cannot be assigned without written consent.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">(b) Force Majeure</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              No liability for delays from causes beyond reasonable control.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">(c) Waiver</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Failure to enforce one provision doesn't waive others.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">(d) Severability</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Invalid provisions are reformed; remaining terms persist.
            </p>

            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">(e) Entire Agreement</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              These are the complete, exclusive agreement superseding prior discussions.
            </p>

          </div>
        </div>
      </section>

      <!-- Investment Disclaimer -->
      <section class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-6 border border-yellow-500/20">
            <h4 class="text-yellow-500 font-semibold mb-2 flex items-center gap-2">
              <span>&#9888;</span> Disclaimer
            </h4>
            <p class="text-gray-400 text-sm leading-relaxed">
              Nothing on this site is investment advice. All information is for informational purposes only. You should not construe any such information or other material as legal, tax, investment, financial, or other advice. Nothing contained on our site constitutes a solicitation, recommendation, endorsement, or offer by esprito.app or any third party service provider to buy or sell any assets, digital coins and tokens, securities or other financial instruments in this or in any other jurisdiction in which such solicitation or offer would be unlawful under the securities laws of such jurisdiction. Please view <a href="/terms.html" class="text-cosmic-400 hover:underline">Terms of Use</a> for more information.
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
              <ul class="space-y-2 text-gray-400">
                <li><a href="/#features" class="hover:text-white transition">Features</a></li>
                <li><a href="/roadmap.html" class="hover:text-white transition">Roadmap</a></li>
                <li><a href="/blog.html" class="hover:text-white transition">Blog</a></li>
              </ul>
            </div>
            <div>
              <h4 class="font-semibold mb-4">Company</h4>
              <ul class="space-y-2 text-gray-400">
                <li><a href="#" class="hover:text-white transition">About</a></li>
                <li><a href="#" class="hover:text-white transition">Careers</a></li>
                <li><a href="#" class="hover:text-white transition">Contact</a></li>
              </ul>
            </div>
            <div>
              <h4 class="font-semibold mb-4">Legal</h4>
              <ul class="space-y-2 text-gray-400">
                <li><a href="/privacy.html" class="hover:text-white transition">Privacy</a></li>
                <li><a href="/terms.html" class="text-white">Terms</a></li>
              </ul>
            </div>
          </div>
          <div class="pt-8 border-t border-white/10 flex flex-col md:flex-row justify-between items-center gap-4">
            <p class="text-gray-400 text-sm">&copy; 2026 Esprito Tech QFZ LLC. All rights reserved.</p>
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

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
</style>
