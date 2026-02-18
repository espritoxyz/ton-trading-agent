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
            Privacy <span class="gradient-text">Policy</span>
          </h1>
          <p class="text-gray-400">Last updated: February 17, 2026</p>
        </div>
      </section>

      <!-- Content -->
      <section class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-8 md:p-12 prose prose-invert">

            <p class="text-gray-300 leading-relaxed mb-8">
              This Privacy Policy ("Policy") describes how Esprito Tech QFZ LLC ("Esprito," "Company," "we," "us," or "our"), a company incorporated under the laws of the State of Qatar and registered in the Ras Bufontas Free Zone, collects, uses, stores, shares, and protects information obtained from users of the Esprito platform, including the website at https://esprito.app/, the Telegram Mini App, mobile applications, APIs, and all related services (collectively, the "Platform" or "Services").
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">1. Introduction</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              We are committed to protecting your privacy and handling your data with transparency. By accessing or using the Platform, you acknowledge that you have read, understood, and consent to the practices described in this Policy. If you do not agree with this Policy, please do not use the Services.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              This Policy should be read in conjunction with our Terms of Use, which govern your use of the Services and are incorporated herein by reference.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">2. Data Controller</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The data controller responsible for the processing of your personal data is:
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              <strong class="text-white">Esprito Tech QFZ LLC</strong><br/>
              Office 62, Ras Bufontas Admin Building, Building 43, Street 517, Zone 49, Ras Bufontas Free Zone, Doha, Qatar<br/>
              Email: support@esprito.app
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">3. Scope of This Policy</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              This Policy applies to all personal data processed in connection with your use of the Platform, regardless of how you access it (web browser, Telegram Mini App, mobile application, or API). This Policy does not apply to the practices of third parties that we do not own or control, including STON.fi, Telegram, TON Blockchain network participants, and other decentralized protocols.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">4. Information We Collect</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.1 Information You Provide Directly</h3>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Wallet Address:</strong> Your public blockchain wallet address, which you connect to the Platform. This is a pseudonymous identifier on the Blockchain.</li>
              <li><strong class="text-white">Telegram Account Data:</strong> If you access the Platform through the Telegram Mini App, we may receive your Telegram user ID, username, first name, last name, language preference, and profile photo.</li>
              <li><strong class="text-white">Trading Commands and Communications:</strong> The text commands, queries, and instructions you submit to the AI Agent.</li>
              <li><strong class="text-white">Subscription and Payment Information:</strong> If you subscribe to a paid tier, we collect information necessary to process your payment. We do not directly store full payment card details.</li>
              <li><strong class="text-white">Support Communications:</strong> Information you provide when contacting our support team.</li>
              <li><strong class="text-white">Feedback and Survey Data:</strong> Any feedback, suggestions, or survey responses you provide.</li>
            </ul>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.2 Information Collected Automatically</h3>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Device and Browser Information:</strong> Device type, operating system, browser type and version, screen resolution, device identifiers, and language settings.</li>
              <li><strong class="text-white">Usage Data:</strong> Pages visited, features used, commands entered (in anonymized/aggregated form), frequency and duration of use.</li>
              <li><strong class="text-white">Log Data:</strong> IP address, access timestamps, referring URLs, error logs, and server response codes.</li>
              <li><strong class="text-white">Location Data:</strong> Approximate geographic location derived from your IP address (country/region level only).</li>
              <li><strong class="text-white">Performance Data:</strong> Platform load times, latency metrics, error rates, and other technical performance indicators.</li>
            </ul>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.3 Blockchain Data (Publicly Available)</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              When you execute transactions through the Platform, certain data is recorded on the public Blockchain: your wallet address, transaction hashes, token amounts, counterparty addresses, timestamps, and Gas Fees paid. This data is not "collected" by Esprito — it is a permanent record on a decentralized network. Esprito may index copies of this publicly available Blockchain data to provide Services.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">5. How We Use Your Information</h2>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Service Delivery:</strong> To provide, operate, and maintain the Platform; to interpret your trading commands via the AI Agent; to execute and manage your Orders; and to provide customer support.</li>
              <li><strong class="text-white">AI and NLP Processing:</strong> To process your text commands through our AI models to interpret your trading intentions and generate appropriate responses.</li>
              <li><strong class="text-white">Analytics and Improvement:</strong> To analyze usage patterns, optimize Platform performance, improve AI model accuracy, and develop new features.</li>
              <li><strong class="text-white">Security and Fraud Prevention:</strong> To detect, investigate, and prevent fraudulent, unauthorized, or illegal activity.</li>
              <li><strong class="text-white">Communications:</strong> To send service-related communications including transaction confirmations, alert notifications, system updates, and security notices.</li>
              <li><strong class="text-white">Legal Compliance:</strong> To comply with applicable laws, including AML, CTF, and sanctions compliance requirements.</li>
              <li><strong class="text-white">Payment Processing:</strong> To process subscription payments and manage your billing account.</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">6. Legal Basis for Processing (GDPR)</h2>
            <p class="text-gray-300 leading-relaxed mb-4">If you are located in the EEA or UK, we rely on the following legal bases:</p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Contract Performance (Article 6(1)(b) GDPR):</strong> Processing necessary for the performance of our contract with you.</li>
              <li><strong class="text-white">Legitimate Interests (Article 6(1)(f) GDPR):</strong> Processing necessary for improving and securing the Platform, conducting analytics, and preventing fraud.</li>
              <li><strong class="text-white">Consent (Article 6(1)(a) GDPR):</strong> Where we rely on your consent (e.g., optional marketing communications), you may withdraw it at any time.</li>
              <li><strong class="text-white">Legal Obligation (Article 6(1)(c) GDPR):</strong> Processing necessary to comply with a legal obligation (e.g., AML/CTF requirements).</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">7. Information Sharing and Disclosure</h2>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">AI Service Providers:</strong> Your trading commands are transmitted to third-party AI providers (e.g., OpenAI) for natural language processing.</li>
              <li><strong class="text-white">Blockchain Networks:</strong> Transaction data is submitted to public Blockchain networks as a necessary part of executing trades.</li>
              <li><strong class="text-white">DEX Protocols:</strong> When you execute trades, your transaction data interacts with third-party DEX smart contracts (e.g., STON.fi).</li>
              <li><strong class="text-white">Payment Processors:</strong> Your payment information is shared with third-party payment processors compliant with PCI-DSS standards.</li>
              <li><strong class="text-white">Analytics and Infrastructure Providers:</strong> We use third-party services for hosting, analytics, monitoring, and infrastructure.</li>
              <li><strong class="text-white">Legal and Regulatory Authorities:</strong> We may disclose your information to law enforcement or regulators when required by law.</li>
              <li><strong class="text-white">Business Transfers:</strong> In the event of a merger, acquisition, or sale of assets, your information may be transferred to the successor entity.</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              We do not sell your personal data to third parties for their own marketing purposes.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">8. Third-Party Services and Integrations</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">STON.fi (Decentralized Exchange)</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito routes trade execution through STON.fi's smart contracts on the TON Blockchain. STON.fi operates as a non-custodial, decentralized protocol. Esprito does not share your personal data (such as your name, email, or IP address) with STON.fi — only on-chain transaction data inherent to the Blockchain interaction is shared.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">Telegram</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              If you access the Platform via the Telegram Mini App, your interaction is facilitated through Telegram's infrastructure. Data shared with Esprito through the Telegram Mini App API is limited to the information described in Section 4.1.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">OpenAI / AI Providers</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Your text commands are processed by AI models hosted by third-party providers. We send the minimum data necessary for command interpretation and have data processing agreements with our AI providers.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">TON Blockchain</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The TON Blockchain is a decentralized public network. Transaction data submitted to TON is publicly accessible and immutable. Esprito has no ability to delete, modify, or restrict access to on-chain data.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">9. Blockchain Data and On-Chain Transparency</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              You acknowledge that the nature of public Blockchain technology means that certain data — including your wallet address, transaction details, token balances, and interactions with smart contracts — is publicly accessible on the Blockchain. While wallet addresses are pseudonymous, it may be possible for third parties to link your wallet address to your identity through Blockchain analytics.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Due to the immutable nature of Blockchain technology, Esprito cannot fulfill requests to delete, modify, or restrict access to data recorded on the Blockchain. The rights described in Section 15 apply only to off-chain data stored on our servers.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">10. AI Processing and Data Use</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">10.1 How AI Processes Your Data</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              When you submit a text command to the Platform, the AI Agent processes your input to understand your intent and generate the appropriate trading action. We do not include your wallet address, IP address, or other identifying information in AI processing requests unless strictly necessary for command interpretation.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">10.2 AI Training and Improvement</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              We may use anonymized and aggregated data derived from User commands to improve the Platform's AI models. We do not use your personal data or identifiable trading commands to train third-party AI models without your explicit consent.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">10.3 Automated Decision-Making</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Significant actions (such as Order execution) require your explicit confirmation before being submitted to the Blockchain. You have the right not to be subject to a decision based solely on automated processing which produces legal or similarly significant effects on you.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">11. Cookies and Tracking Technologies</h2>
            <p class="text-gray-300 leading-relaxed mb-4">The Platform may use the following types of cookies:</p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Strictly Necessary Cookies:</strong> Essential for the Platform to function properly. These cannot be disabled.</li>
              <li><strong class="text-white">Functional Cookies:</strong> Used to remember your preferences, language settings, and interface customizations.</li>
              <li><strong class="text-white">Analytics Cookies:</strong> Used to collect information about how you interact with the Platform.</li>
              <li><strong class="text-white">Performance Cookies:</strong> Used to monitor Platform performance and error tracking.</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              Where required by applicable law, we will obtain your consent before placing non-essential cookies. You can manage your cookie preferences through the cookie consent banner or your browser settings.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">12. Data Retention</h2>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Account and Wallet Data:</strong> Retained for the duration of your active use and for five (5) years thereafter, or as required by applicable law.</li>
              <li><strong class="text-white">Transaction History (Off-Chain):</strong> Retained for five (5) years from the date of the transaction.</li>
              <li><strong class="text-white">Trading Commands and AI Interaction Logs:</strong> Retained for twelve (12) months, after which they are anonymized or deleted.</li>
              <li><strong class="text-white">Support Communications:</strong> Retained for three (3) years from the date of resolution.</li>
              <li><strong class="text-white">Analytics and Usage Data:</strong> Retained in anonymized/aggregated form indefinitely; identifiable usage data retained for twenty-four (24) months.</li>
              <li><strong class="text-white">Blockchain Data:</strong> On-chain data is immutable and permanent. Esprito cannot delete data recorded on the Blockchain.</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">13. Data Security</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              We implement appropriate technical and organizational security measures, including:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Encryption of data in transit (TLS/SSL) and at rest</li>
              <li>Access controls and authentication mechanisms for internal systems</li>
              <li>Regular security assessments, penetration testing, and vulnerability scanning</li>
              <li>Smart contract audits conducted by independent third-party auditors</li>
              <li>Incident response procedures and breach notification protocols</li>
              <li>Employee training on data protection and security best practices</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              While we strive to protect your personal data, no method of transmission over the Internet is 100% secure. You are responsible for maintaining the security of your Wallet, private keys, and access credentials.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">14. International Data Transfers</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Your personal data may be transferred to, stored, and processed in countries other than your country of residence. When we transfer personal data internationally, we implement appropriate safeguards, which may include Standard Contractual Clauses (SCCs) approved by the European Commission or other appropriate safeguards as permitted by applicable law.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">15. Your Rights</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Depending on your location and applicable data protection laws (including GDPR, UK GDPR, CCPA/CPRA), you may have the following rights:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Right of Access:</strong> Request a copy of the personal data we hold about you.</li>
              <li><strong class="text-white">Right to Rectification:</strong> Request the correction of inaccurate or incomplete personal data.</li>
              <li><strong class="text-white">Right to Erasure:</strong> Request the deletion of your personal data, subject to certain exceptions. This right does not extend to data recorded on the Blockchain.</li>
              <li><strong class="text-white">Right to Restriction of Processing:</strong> Request that we restrict processing of your personal data in certain circumstances.</li>
              <li><strong class="text-white">Right to Data Portability:</strong> Receive your personal data in a structured, commonly used, and machine-readable format.</li>
              <li><strong class="text-white">Right to Object:</strong> Object to the processing of your personal data based on our legitimate interests or for direct marketing purposes.</li>
              <li><strong class="text-white">Right to Withdraw Consent:</strong> Withdraw consent at any time, without affecting the lawfulness of prior processing.</li>
              <li><strong class="text-white">California Privacy Rights (CCPA/CPRA):</strong> If you are a California resident, you have additional rights including the right to know, delete, opt-out of sale/sharing, and non-discrimination.</li>
            </ul>
            <p class="text-gray-300 leading-relaxed mb-4">
              To exercise any of these rights, please contact us at support@esprito.app. We will respond within the timeframe required by applicable law (typically within 30 days). You also have the right to lodge a complaint with your local data protection supervisory authority.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">16. Children's Privacy</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Platform is not intended for individuals under the age of eighteen (18) or the age of legal majority in their jurisdiction, whichever is higher. If we become aware that we have collected personal data from a minor without appropriate parental or guardian consent, we will take reasonable steps to delete such data.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">17. Changes to This Privacy Policy</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              We may update this Privacy Policy from time to time. When we make material changes, we will notify you by posting the updated Policy with a revised "Last Updated" date and, where required by law, by providing additional notice. Your continued use of the Platform after the effective date of the updated Policy constitutes your acceptance of the changes.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">18. Contact Information</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              If you have any questions, concerns, or requests regarding this Privacy Policy, please contact us at:
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              <strong class="text-white">Esprito Tech QFZ LLC</strong><br/>
              Office 62, Ras Bufontas Admin Building, Building 43, Street 517, Zone 49, Ras Bufontas Free Zone, Doha, Qatar<br/>
              Email: support@esprito.app<br/>
              Website: https://esprito.app/
            </p>
            <p class="text-gray-300 leading-relaxed mt-8 uppercase text-sm">
              BY USING THE ESPRITO PLATFORM, YOU ACKNOWLEDGE THAT YOU HAVE READ AND UNDERSTOOD THIS PRIVACY POLICY.
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
              Nothing on this site is investment advice. All information is for informational purposes only. You should not construe any such information or other material as legal, tax, investment, financial, or other advice. Nothing contained on our site constitutes a solicitation, recommendation, endorsement, or offer by esprito.app or any third party service provider to buy or sell any assets, digital coins and tokens, securities or other financial instruments in this or in any other jurisdiction in which such solicitation or offer would be unlawful under the securities laws of such jurisdiction. Please view <a href="/terms" class="text-cosmic-400 hover:underline">Terms of Use</a> for more information.
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
                <li><a href="#" class="hover:text-white transition">Documentation</a></li>
              </ul>
            </div>

            <div>
              <h4 class="font-semibold mb-4">Company</h4>
              <ul class="space-y-2 text-gray-400 text-sm">
                <li><a href="/about" class="hover:text-white transition">About Us</a></li>
                <li><a href="/blog" class="hover:text-white transition">Blog</a></li>
                <li><a href="/careers" class="hover:text-white transition">Careers</a></li>
                <li><a href="mailto:support@esprito.app" class="hover:text-white transition">Contact</a></li>
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

@keyframes gradientFlow {
  0% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
  100% { background-position: 0% 50%; }
}
</style>
