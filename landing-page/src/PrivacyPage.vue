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
              <span class="text-2xl font-bold gradient-text">Esprito AI</span>
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
            Privacy <span class="gradient-text">Policy</span>
          </h1>
          <p class="text-gray-400">Last updated: January 2026</p>
        </div>
      </section>

      <!-- Content -->
      <section class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-8 md:p-12 prose prose-invert">

            <p class="text-gray-300 leading-relaxed mb-8">
              This Privacy Policy describes how your personal information is collected, used, and shared when you visit or use esprito.app (the "Site").
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Personal Information We Collect</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              When you visit the Site, we automatically collect certain information about your device, including information about your web browser, IP address, time zone, and some of the cookies that are installed on your device.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Additionally, as you browse the Site, we collect information about the individual web pages or products that you view, what websites or search terms referred you to the Site, and information about how you interact with the Site. We refer to this automatically-collected information as "Device Information".
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              We collect Device Information using the following technologies:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong>"Cookies"</strong> are data files that are placed on your device or computer and often include an anonymous unique identifier.</li>
              <li><strong>"Log files"</strong> track actions occurring on the Site, and collect data including your IP address, browser type, Internet service provider, referring/exit pages, and date/time stamps.</li>
              <li><strong>"Web beacons", "tags", and "pixels"</strong> are electronic files used to record information about how you browse the Site.</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              Additionally when you contact us through the contact form(s) located at the Site, we collect certain information from you, including your name, email address, and phone number. We refer to this information as "Contact Information".
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              When we talk about "Personal Information" in this Privacy Policy, we are talking both about Device Information and Contact Information.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">How Do We Use Your Personal Information?</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              We use the Contact Information to:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Communicate with you;</li>
              <li>When in line with the preferences you have shared with us, provide you with information or advertising relating to our products or services.</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              We use the Device Information that we collect to help us screen for potential risk and fraud (in particular, your IP address), and more generally to improve and optimize our Site (for example, by generating analytics about how our customers browse and interact with the Site, and to assess the success of our marketing and advertising campaigns).
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Sharing Your Personal Information</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              We share your Personal Information with third parties to help us use your Personal Information, as described above. We use Google Analytics to help us understand how our customers use the Site -- you can read more about how Google uses your Personal Information here: <a href="https://www.google.com/intl/en/policies/privacy/" target="_blank" class="text-cosmic-400 hover:underline">https://www.google.com/intl/en/policies/privacy/</a>. You can opt-out of Google Analytics here: <a href="https://tools.google.com/dlpage/gaoptout" target="_blank" class="text-cosmic-400 hover:underline">https://tools.google.com/dlpage/gaoptout</a>.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Finally, we may also share your Personal Information to comply with applicable laws and regulations, to respond to a subpoena, search warrant or other lawful request for information we receive, or to otherwise protect our rights.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Behavioural Advertising</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              As described above, we use your Personal Information to provide you with targeted advertisements or marketing communications we believe may be of interest to you.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              You can opt out of targeted advertising by using the links below:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Facebook: <a href="https://www.facebook.com/settings/?tab=ads" target="_blank" class="text-cosmic-400 hover:underline">https://www.facebook.com/settings/?tab=ads</a></li>
              <li>Google: <a href="https://www.google.com/settings/ads/anonymous" target="_blank" class="text-cosmic-400 hover:underline">https://www.google.com/settings/ads/anonymous</a></li>
              <li>Bing: <a href="https://about.ads.microsoft.com/en-us/resources/policies/personalized-ads" target="_blank" class="text-cosmic-400 hover:underline">https://about.ads.microsoft.com/en-us/resources/policies/personalized-ads</a></li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              Additionally, you can opt out of some of these services by visiting the Digital Advertising Alliance's opt-out portal at: <a href="http://optout.aboutads.info/" target="_blank" class="text-cosmic-400 hover:underline">http://optout.aboutads.info/</a>.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Do Not Track</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Please note that we do not alter our Site's data collection and use practices when we see a Do Not Track signal from your browser.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Your Rights</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              If you are a European resident, you have the right to access personal information we hold about you and to ask that your personal information be corrected, updated, or deleted. If you would like to exercise this right, please contact us through the contact information below.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Additionally, if you are a European resident we note that we are processing your information in order to contact you, or otherwise to pursue our legitimate business interests listed above. Additionally, please note that your information will be transferred outside of Europe, including to Canada and the United States.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Data Retention</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              When you contact us through the Site, we will maintain your Contact Information for our records unless and until you ask us to delete this information.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Changes</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              We may update this privacy policy from time to time in order to reflect, for example, changes to our practices or for other operational, legal or regulatory reasons.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">Contact Us</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              For more information about our privacy practices, if you have questions, or if you would like to make a complaint, please contact us at <a href="mailto:privacy@esprito.app" class="text-cosmic-400 hover:underline">privacy@esprito.app</a>.
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
        <div class="max-w-7xl mx-auto text-center">
          <p class="text-gray-400 text-sm mb-4">
            &copy; 2026 Esprito AI. All rights reserved.
          </p>
          <div class="flex justify-center gap-6 text-sm text-gray-500">
            <a href="/privacy.html" class="text-white">Privacy Policy</a>
            <a href="/terms.html" class="hover:text-white transition">Terms of Service</a>
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
