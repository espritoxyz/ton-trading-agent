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
          <p class="text-gray-400">Last updated: February 17, 2026</p>
        </div>
      </section>

      <!-- Content -->
      <section class="px-4 sm:px-6 lg:px-8 pb-12">
        <div class="max-w-4xl mx-auto">
          <div class="glass-card p-8 md:p-12 prose prose-invert">

            <p class="text-gray-300 leading-relaxed mb-6 p-4 border border-yellow-500/30 rounded-lg bg-yellow-500/5">
              <strong class="text-yellow-400">RISK WARNING:</strong> Trading in digital assets and cryptocurrencies involves substantial risk of loss and is not suitable for every person. The value of digital assets can be extremely volatile. Esprito does not provide financial, investment, tax, or legal advice. Past performance is not indicative of future results.
            </p>
            <p class="text-gray-300 leading-relaxed mb-6 p-4 border border-red-500/30 rounded-lg bg-red-500/5">
              <strong class="text-red-400">IMPORTANT NOTICE:</strong> These Terms contain a binding arbitration clause and a class action waiver in Section 18. By using the Services, you agree to resolve disputes through individual arbitration and waive any right to participate in class actions.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">1. Introduction and Acceptance of Terms</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              These Terms of Use ("Terms") constitute a legally binding agreement between you ("User," "you," or "your") and Esprito Tech QFZ LLC ("Esprito," "Company," "we," "us," or "our"), a company incorporated under the laws of the State of Qatar and registered in the Ras Bufontas Free Zone, with its registered address at Office 62, Ras Bufontas Admin Building, Building 43, Street 517, Zone 49, Ras Bufontas Free Zone, Doha, Qatar.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              By accessing, browsing, or using the Platform, connecting a wallet, placing any order, or otherwise interacting with the Services, you acknowledge that you have read, understood, and agree to be bound by these Terms, together with our Privacy Policy, which is incorporated herein by reference. If you do not agree to these Terms, you must immediately cease all use of the Platform.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">2. Definitions</h2>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">"AI Agent" or "AI Assistant"</strong> means the artificial intelligence-powered natural language processing system integrated into the Platform.</li>
              <li><strong class="text-white">"Blockchain"</strong> means a distributed digital ledger technology, including the TON network and other supported blockchains.</li>
              <li><strong class="text-white">"DEX" or "Decentralized Exchange"</strong> means a decentralized protocol facilitating peer-to-peer trading of digital assets, including STON.fi.</li>
              <li><strong class="text-white">"Digital Assets"</strong> means cryptocurrencies, tokens, coins, stablecoins, and other digital representations of value, including TON, USDT, NOT, STON, and other tradeable assets.</li>
              <li><strong class="text-white">"Gas Fees"</strong> means the network transaction fees required by the Blockchain, paid directly to the network and not to Esprito.</li>
              <li><strong class="text-white">"Order"</strong> means any instruction to buy, sell, swap, or otherwise transact in Digital Assets, including limit orders, stop-loss orders, take-profit orders, and market orders.</li>
              <li><strong class="text-white">"Session Keys"</strong> means temporary cryptographic keys generated through Account Abstraction mechanisms that allow the Platform to execute pre-authorized transactions within defined parameters.</li>
              <li><strong class="text-white">"Strategy"</strong> means a pre-configured or User-defined automated trading plan, including DCA, grid trading, averaging, and other algorithmic trading approaches.</li>
              <li><strong class="text-white">"Wallet"</strong> means a User's self-custodial blockchain wallet that interfaces with the Platform for signing and authorizing transactions.</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">3. Eligibility</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">3.1 Age and Legal Capacity</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              You must be at least eighteen (18) years of age, or the age of legal majority in your jurisdiction (whichever is higher), to access or use the Services. By using the Platform, you represent and warrant that you meet this age requirement and have the full legal capacity to enter into a binding agreement.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">3.2 Jurisdictional Restrictions</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Services are not available to persons or entities located in, incorporated in, or residents of any jurisdiction where the use of decentralized exchanges, cryptocurrency trading, or the Services would be prohibited by applicable law. Restricted Jurisdictions currently include, but are not limited to: the United States of America (where specific Services may be restricted), Cuba, Iran, North Korea, Syria, the Crimea region, the Donetsk People's Republic, and the Luhansk People's Republic.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">3.3 Compliance Responsibility</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              You are solely responsible for determining whether your use of the Services complies with all applicable laws, regulations, and governmental orders in your jurisdiction, including securities laws, AML laws, CTF requirements, and tax obligations.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">4. Description of Services</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.1 Platform Overview</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito is an AI-powered trading assistant for decentralized exchanges that provides a natural language interface to interact with DeFi protocols. The Platform enables Users to place and manage trades, set automated orders, receive market alerts, create trading strategies, and access analytics tools — all through plain-language text commands.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.2 Core Functionality</h3>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>AI-powered natural language interface for interpreting and executing trading commands in multiple languages</li>
              <li>Automated order types, including limit orders, stop-loss, and take-profit orders</li>
              <li>Customizable price, volume, and news-based alert notifications</li>
              <li>Pre-built and customizable trading strategies, including DCA, grid trading, and averaging</li>
              <li>Chart visualization with technical indicators (RSI, MACD, and others) and AI-generated trading signals</li>
              <li>Integration with DEX protocols (currently STON.fi on the TON Blockchain) for trade execution</li>
              <li>Telegram Mini App and web application interfaces</li>
            </ul>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.3 Non-Custodial Nature</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito is a non-custodial platform. At no time does the Company take custody, possession, or control of your Digital Assets. All Digital Assets remain in your Wallet at all times. You retain sole control over your private keys and Wallet.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">4.4 No Financial Advice</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito does not provide investment advice, financial advice, trading advice, or any other professional advice. Any information provided through the Platform, including AI-generated signals, analytics, and chart data, is for informational purposes only and should not be construed as a recommendation to buy, sell, or hold any Digital Asset.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">5. Account and Wallet Access</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">5.1 Wallet Connection</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              To use the Services, you must connect a compatible self-custodial Wallet (e.g., Tonkeeper, TON Wallet, or other supported wallets). Your Wallet address serves as your unique identifier on the Platform.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">5.2 Telegram Authentication</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Where the Platform is accessed through the Telegram Mini App, authentication may be facilitated through your Telegram account. You are responsible for maintaining the security of your Telegram account and any linked wallet connections.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">5.3 Session Keys and Delegated Authority</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Platform may utilize Session Keys to execute pre-authorized transactions on your behalf within the parameters you define (e.g., specific token pairs, maximum amounts, time limits). You may revoke Session Keys at any time through the Platform interface.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">5.4 Security</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              You are solely responsible for maintaining the security of your Wallet credentials, private keys, seed phrases, and Telegram account. Esprito shall not be liable for any loss or damage arising from your failure to maintain the security of your access credentials.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">6. AI-Powered Features and Limitations</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">6.1 Natural Language Processing</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Platform uses artificial intelligence, including large language models and specialized intent classification models, to interpret your text commands. You acknowledge and agree that:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>The AI Agent may misinterpret or incorrectly parse your commands, particularly those that are ambiguous, incomplete, or contain typographical errors</li>
              <li>You are solely responsible for reviewing and confirming all Orders before execution</li>
              <li>The AI Agent's interpretation of your command does not constitute financial advice</li>
              <li>AI-generated trading signals and insights are probabilistic and carry no guarantee of accuracy or profitability</li>
            </ul>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">6.2 Confirmation Mechanism</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Platform may present a summary of the interpreted Order for your confirmation before execution. You agree to carefully review each such summary and take full responsibility for confirming and submitting any Order. Once an Order is confirmed and submitted to the Blockchain, it may be irreversible.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">7. Trading and Order Execution</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">7.1 Order Submission</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              When you submit an Order through the Platform, the Platform transmits the corresponding transaction(s) to the relevant Blockchain and/or DEX protocol. You understand and acknowledge that:
            </p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Esprito acts solely as a technological intermediary and does not act as a counterparty, broker, dealer, exchange, or market maker</li>
              <li>Order execution depends on Blockchain network conditions, DEX liquidity, gas availability, and smart contract functionality — none of which are within Esprito's control</li>
              <li>Orders may be partially filled, unfilled, delayed, or fail due to network congestion, insufficient liquidity, price slippage, or other factors beyond our control</li>
              <li>All transactions on the Blockchain are final and irreversible once confirmed. Esprito cannot reverse, cancel, or modify confirmed transactions</li>
            </ul>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">7.2 Slippage and Price Impact</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Trading on decentralized exchanges is subject to slippage. You are responsible for setting appropriate slippage tolerance parameters. Esprito is not responsible for losses resulting from slippage, price impact, front-running, sandwich attacks, or other forms of MEV.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">7.3 Failed Transactions</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              If a transaction fails on the Blockchain, Gas Fees may still be charged by the network. Esprito is not responsible for Gas Fees incurred in connection with failed transactions.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">8. Integration with Third-Party Protocols (STON.fi)</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              The Platform integrates with STON.fi, an independent, non-custodial decentralized exchange operated on the TON Blockchain. Esprito is not affiliated with, endorsed by, or responsible for the operation of STON.fi or any other third-party protocol.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              When you execute trades through STON.fi via the Platform: (a) your funds interact directly with STON.fi smart contracts; (b) swap fees are charged by the STON.fi protocol (typically 0.3%, of which 0.2% goes to liquidity providers); (c) the availability and pricing of token pairs depend on STON.fi's liquidity pools. Esprito charges separate fees as described in Section 9.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">9. Fees and Payments</h2>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Transaction Fees:</strong> Esprito charges a transaction fee calculated as a percentage of each trade executed through the Platform, depending on your subscription tier.</li>
              <li><strong class="text-white">Gas Fees:</strong> All Blockchain transactions require Gas Fees paid directly to the Blockchain network. These are outside Esprito's control.</li>
              <li><strong class="text-white">Third-Party Protocol Fees:</strong> Third-party protocols (such as STON.fi) may charge their own fees, separate from and in addition to Esprito's fees.</li>
              <li><strong class="text-white">Fee Changes:</strong> Esprito reserves the right to modify its fee structure at any time with notice through the Platform.</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">10. Subscription Tiers</h2>
            <div class="overflow-x-auto mb-6">
              <table class="w-full text-sm text-gray-300 border-collapse">
                <thead>
                  <tr class="border-b border-white/20">
                    <th class="text-left py-3 px-4 text-white font-semibold">Tier</th>
                    <th class="text-left py-3 px-4 text-white font-semibold">Monthly Fee</th>
                    <th class="text-left py-3 px-4 text-white font-semibold">Transaction Fee</th>
                    <th class="text-left py-3 px-4 text-white font-semibold">Included Functionality</th>
                  </tr>
                </thead>
                <tbody>
                  <tr class="border-b border-white/10">
                    <td class="py-3 px-4 font-semibold text-white">Free</td>
                    <td class="py-3 px-4">$0</td>
                    <td class="py-3 px-4">0.15%</td>
                    <td class="py-3 px-4">Basic commands, simple orders, standard notifications, basic charts</td>
                  </tr>
                  <tr class="border-b border-white/10">
                    <td class="py-3 px-4 font-semibold text-white">Basic</td>
                    <td class="py-3 px-4">$30/month</td>
                    <td class="py-3 px-4">0.10%</td>
                    <td class="py-3 px-4">Everything in Free + advanced strategies (DCA, grids), priority alerts, up to 10 concurrent strategies</td>
                  </tr>
                  <tr>
                    <td class="py-3 px-4 font-semibold text-white">Premium</td>
                    <td class="py-3 px-4">$80/month</td>
                    <td class="py-3 px-4">0.05%</td>
                    <td class="py-3 px-4">Everything in Basic + unlimited strategies, AI signals, external API access, dedicated 24/7 support</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <p class="text-gray-300 leading-relaxed mb-4">
              Paid subscriptions are billed monthly in advance. All subscription fees are non-refundable unless otherwise required by applicable law. You may upgrade, downgrade, or cancel your subscription at any time through the Platform interface. Changes take effect at the beginning of the next billing cycle.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">11. User Conduct and Prohibited Activities</h2>
            <p class="text-gray-300 leading-relaxed mb-4">You agree that you will NOT:</p>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li>Use the Services for money laundering, terrorist financing, sanctions evasion, fraud, or any other illegal activity</li>
              <li>Manipulate markets, engage in wash trading, spoofing, layering, front-running, or any other form of market manipulation</li>
              <li>Attempt to exploit, hack, reverse-engineer, decompile, or disassemble any part of the Platform, its AI models, or underlying infrastructure</li>
              <li>Use bots, scrapers, crawlers, or other automated tools to access the Platform in an unauthorized manner</li>
              <li>Circumvent any security features, access controls, rate limits, or geographical restrictions</li>
              <li>Impersonate any person or entity, or misrepresent your affiliation with any person or entity</li>
              <li>Use the Services to trade or transfer Digital Assets that are stolen, fraudulently obtained, or otherwise illegally acquired</li>
              <li>Resell, sublicense, or provide access to the Services to third parties without express written authorization from Esprito</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">12. Risks and Disclaimers</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">12.1 Digital Asset Risks</h3>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Market Risk:</strong> The value of Digital Assets is highly volatile and can decline rapidly, including to zero.</li>
              <li><strong class="text-white">Liquidity Risk:</strong> DEX liquidity pools may have insufficient liquidity for your trades, leading to significant slippage or inability to execute orders.</li>
              <li><strong class="text-white">Smart Contract Risk:</strong> Smart contracts may contain bugs, vulnerabilities, or exploits that could result in the loss of Digital Assets.</li>
              <li><strong class="text-white">Regulatory Risk:</strong> The legal and regulatory status of Digital Assets and DeFi protocols may change at any time, potentially affecting your ability to use the Services.</li>
              <li><strong class="text-white">Technology Risk:</strong> The Platform relies on Blockchain networks, third-party APIs, AI models, and internet connectivity. Failures in any of these components may result in losses.</li>
              <li><strong class="text-white">MEV and Front-Running Risk:</strong> Transactions on public Blockchains may be subject to front-running, sandwich attacks, or other forms of MEV extraction.</li>
              <li><strong class="text-white">Key Management Risk:</strong> Loss of your Wallet private keys, seed phrase, or access credentials may result in permanent and irreversible loss of your Digital Assets.</li>
            </ul>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">12.2 Disclaimer of Warranties</h3>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase text-sm">
              THE PLATFORM AND SERVICES ARE PROVIDED ON AN "AS IS" AND "AS AVAILABLE" BASIS WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED. TO THE FULLEST EXTENT PERMITTED BY APPLICABLE LAW, ESPRITO DISCLAIMS ALL WARRANTIES, INCLUDING BUT NOT LIMITED TO IMPLIED WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, TITLE, AND NON-INFRINGEMENT. ESPRITO DOES NOT WARRANT THAT THE SERVICES WILL BE UNINTERRUPTED, ERROR-FREE, OR SECURE.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">13. Intellectual Property</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              All intellectual property rights in the Platform, including software, code, algorithms, AI models, user interface designs, trademarks, logos, and content, are owned by or licensed to Esprito. Nothing in these Terms grants you any right, title, or interest in the Platform's intellectual property, except for the limited license to use the Services as expressly provided herein.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Subject to your compliance with these Terms, Esprito grants you a limited, non-exclusive, non-transferable, revocable license to access and use the Platform solely for your personal, non-commercial trading purposes.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">14. Limitation of Liability</h2>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase text-sm">
              TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, IN NO EVENT SHALL ESPRITO, ITS AFFILIATES, DIRECTORS, OFFICERS, EMPLOYEES, AGENTS, OR LICENSORS BE LIABLE FOR ANY INDIRECT, INCIDENTAL, SPECIAL, CONSEQUENTIAL, OR PUNITIVE DAMAGES, INCLUDING BUT NOT LIMITED TO LOSS OF PROFITS, LOSS OF DATA, LOSS OF DIGITAL ASSETS, BUSINESS INTERRUPTION, OR PERSONAL INJURY, ARISING OUT OF OR IN CONNECTION WITH YOUR USE OF OR INABILITY TO USE THE SERVICES.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase text-sm">
              TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, ESPRITO'S TOTAL AGGREGATE LIABILITY FOR ALL CLAIMS SHALL NOT EXCEED THE GREATER OF (A) THE TOTAL FEES PAID BY YOU TO ESPRITO IN THE TWELVE (12) MONTHS IMMEDIATELY PRECEDING THE EVENT GIVING RISE TO THE CLAIM, OR (B) ONE HUNDRED US DOLLARS ($100 USD).
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">15. Indemnification</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              You agree to indemnify, defend, and hold harmless Esprito, its affiliates, directors, officers, employees, agents, and licensors from and against any and all claims, damages, losses, liabilities, costs, and expenses (including reasonable attorneys' fees) arising out of or related to: (a) your use of or access to the Services; (b) your violation of these Terms; (c) your violation of any applicable law, rule, or regulation; (d) your violation of any rights of a third party; (e) any Order placed or transaction executed through your Wallet; or (f) any dispute between you and a third party arising from your use of the Services.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">16. Modifications to Terms and Services</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito reserves the right to modify, amend, or supplement these Terms at any time at its sole discretion. Material changes will be communicated through the Platform interface or by posting the updated Terms with a revised "Last Updated" date. Your continued use of the Services after the effective date of any modifications constitutes your acceptance of the modified Terms.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito reserves the right to modify, suspend, or discontinue any part of the Services at any time, with or without notice, including adding or removing supported Blockchains, DEX integrations, order types, strategies, or features.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">17. Termination</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              You may terminate your use of the Services at any time by disconnecting your Wallet, canceling any active subscriptions, and ceasing to access the Platform.
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              Esprito may, at its sole discretion and without prior notice, suspend, restrict, or terminate your access to the Services if you breach any provision of these Terms, if required to do so by applicable law, or if we decide to discontinue the Services. Upon termination, all licenses and rights granted to you under these Terms shall immediately cease. Your Digital Assets remain in your Wallet and are accessible through other means independent of the Platform.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">18. Governing Law and Dispute Resolution</h2>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">18.1 Governing Law</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              These Terms shall be governed by and construed in accordance with the laws of the State of Qatar, without regard to its conflict of law principles.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">18.2 Binding Arbitration</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Any dispute, controversy, or claim arising out of or relating to these Terms or the Services shall be finally resolved by binding arbitration administered by the Qatar International Centre for Conciliation and Arbitration (QICCA), or alternatively the International Chamber of Commerce (ICC). The arbitration shall be conducted by a single arbitrator, in the English language, with the seat of arbitration in Doha, Qatar.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">18.3 Class Action Waiver</h3>
            <p class="text-gray-300 leading-relaxed mb-4 uppercase text-sm">
              YOU AGREE THAT ANY DISPUTE RESOLUTION PROCEEDINGS WILL BE CONDUCTED ONLY ON AN INDIVIDUAL BASIS AND NOT IN A CLASS, CONSOLIDATED, OR REPRESENTATIVE ACTION. YOU WAIVE ANY RIGHT TO PARTICIPATE IN A CLASS ACTION LAWSUIT OR CLASS-WIDE ARBITRATION.
            </p>
            <h3 class="text-xl font-semibold mt-6 mb-3 text-white">18.4 Limitation Period</h3>
            <p class="text-gray-300 leading-relaxed mb-4">
              Any cause of action or claim arising out of or relating to these Terms or the Services must be commenced within one (1) year after the cause of action accrues. Otherwise, such cause of action or claim is permanently barred.
            </p>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">19. Miscellaneous Provisions</h2>
            <ul class="list-disc list-inside text-gray-300 mb-4 space-y-2">
              <li><strong class="text-white">Entire Agreement:</strong> These Terms, together with the Privacy Policy and any other policies referenced herein, constitute the entire agreement between you and Esprito with respect to the Services.</li>
              <li><strong class="text-white">Severability:</strong> If any provision of these Terms is held to be invalid or unenforceable, the remaining provisions shall continue in full force and effect.</li>
              <li><strong class="text-white">Waiver:</strong> The failure of Esprito to exercise or enforce any right or provision of these Terms shall not constitute a waiver of such right or provision.</li>
              <li><strong class="text-white">Assignment:</strong> You may not assign or transfer these Terms or any rights hereunder without Esprito's prior written consent. Esprito may freely assign these Terms without restriction.</li>
              <li><strong class="text-white">Force Majeure:</strong> Esprito shall not be liable for any failure or delay in performance resulting from causes beyond its reasonable control, including Blockchain network outages, smart contract failures, acts of God, war, terrorism, pandemics, or governmental actions.</li>
              <li><strong class="text-white">Language:</strong> These Terms are drafted in English. In the event of any inconsistency between the English version and any translated version, the English version shall prevail.</li>
            </ul>

            <h2 class="text-2xl font-bold mt-8 mb-4 gradient-text">20. Contact Information</h2>
            <p class="text-gray-300 leading-relaxed mb-4">
              If you have any questions, concerns, or complaints regarding these Terms or the Services, please contact us at:
            </p>
            <p class="text-gray-300 leading-relaxed mb-4">
              <strong class="text-white">Esprito Tech QFZ LLC</strong><br/>
              Office 62, Ras Bufontas Admin Building, Building 43, Street 517, Zone 49, Ras Bufontas Free Zone, Doha, Qatar<br/>
              Email: support@esprito.app<br/>
              Website: https://esprito.app/
            </p>
            <p class="text-gray-300 leading-relaxed mt-8 uppercase text-sm">
              BY USING THE ESPRITO PLATFORM, YOU ACKNOWLEDGE THAT YOU HAVE READ, UNDERSTOOD, AND AGREE TO BE BOUND BY THESE TERMS OF USE.
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
</style>
