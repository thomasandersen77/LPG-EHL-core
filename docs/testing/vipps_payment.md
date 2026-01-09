



Dette ser veldig bra ut som neste steg – Vipps passer fint inn i arkitekturen du allerede har laget (API-gateway, emulator, Postgres, Azure-sync).

Jeg deler svaret i to:
1.	Forslag til betalingsflyt med Vipps (konseptuelt)
2.	Konkrete implementasjons­forslag for Kotlin + React (som Warp kan “vibe-kode” videre på)

⸻

1. Betalingsflyt med Vipps / MobilePay (ePayment API)

Vipps anbefaler i dag ePayment API for nye integrasjoner, ikke den gamle eCom-API’en. ￼

1.1. Hva dere trenger fra Vipps

Når Norgesgass får avtale med Vipps, får dere i test-miljøet:
•	client_id
•	client_secret
•	Ocp-Apim-Subscription-Key
•	merchantSerialNumber (MSN / “sales unit”) ￼

Og dere kan bruke Merchant Test (MT)-miljøet, med Vipps test-appen på mobilen. ￼

Base-URL: https://apitest.vipps.no (prod er https://api.vipps.no). ￼

1.2. Standard ePayment-flyt (tilpasset pumpa)

Forenklet flyt tilpasset “kunde står ved pumpa og vil betale med Vipps”:
1.	Kunde velger Vipps i UI
•	Operatør/kunde skriver inn mobilnummer i frontend.
•	Frontend kaller POST /api/payments/vipps/start.
2.	Backend oppretter betaling i Vipps
•	Henter/bruker access token via POST /accesstoken/get. ￼
•	Kaller POST /epayment/v1/payments med:
•	amount (f.eks. maksbeløp 2000 NOK)
•	paymentMethod.type = "WALLET"
•	customer.phoneNumber
•	reference (intern ordre-/transaksjons-id)
•	returnUrl (kan peke til en enkel “betaling fullført” side)
•	userFlow – for kiosk kan dere bruke web-redirect eller annen passende flow. ￼
3.	Vipps sender push til kunden
•	Kunden får push i Vipps-appen, godkjenner betalingen. ￼
4.	Backend får beskjed når betalingen er klar
•	Enten via polling (GET /epayment/v1/payments/{reference})
•	Eller via webhook (/webhooks/v1/webhooks + ePayment events) inn mot en HTTP-endpoint i Azure. ￼
5.	Edge/pumpe får grønt lys
•	Din eksisterende arkitektur kan:
•	Lytte på Azure-queue / statusendring, eller
•	Pollen API’et (/api/payments/{id}) fra ARK-3600.
•	Når status = AUTHORIZED/RESERVED, sender edge UNBLOCK til dispenser og starter fylling.
6.	Capture / sluttoppgjør
•	Etter fylling (liter og faktisk beløp kjent):
•	Backend kaller capture-endepunktet i ePayment for å hente inn riktig beløp (under eller lik autorisert beløp). ￼

⸻

2. Konkrete forslag til implementasjon (Kotlin + React)

2.1. Backend: PaymentGateway + VippsPaymentGateway

Du har allerede et PaymentGateway-interface. Lag en Vipps-implementasjon som bruker ePayment-API’et.

Interface (forenklet):

interface PaymentGateway {
fun startPayment(
transactionId: UUID,
amountInNok: Long,
phoneNumber: String
): PaymentInfo

    fun getPaymentStatus(transactionId: UUID): PaymentStatus
}

Vipps-konfig (application.yml):

vipps:
base-url: https://apitest.vipps.no
client-id: ${VIPPS_CLIENT_ID}
client-secret: ${VIPPS_CLIENT_SECRET}
subscription-key: ${VIPPS_SUBSCRIPTION_KEY}
merchant-serial-number: ${VIPPS_MSN}

Access token cache (Kotlin pseudokode):

@Service
class VippsAccessTokenService(
private val webClient: WebClient,
private val config: VippsConfig
) {
@Volatile
private var cachedToken: AccessToken? = null

    fun getToken(): String {
        val token = cachedToken
        if (token != null && token.expiresAt.isAfter(Instant.now().plusSeconds(30))) {
            return token.value
        }

        val response = webClient.post()
            .uri("${config.baseUrl}/accesstoken/get")
            .header("client_id", config.clientId)
            .header("client_secret", config.clientSecret)
            .header("Ocp-Apim-Subscription-Key", config.subscriptionKey)
            .header("Merchant-Serial-Number", config.merchantSerialNumber)
            .retrieve()
            .bodyToMono<VippsAccessTokenResponse>()
            .block()!!

        val expiresAt = Instant.now().plusSeconds(response.expiresIn)
        cachedToken = AccessToken(response.accessToken, expiresAt)
        return response.accessToken
    }
}

VippsPaymentGateway.startPayment (pseudokode):

@Service
@Profile("vipps")
class VippsPaymentGateway(
private val webClient: WebClient,
private val tokenService: VippsAccessTokenService,
private val config: VippsConfig,
private val paymentsRepository: PaymentsRepository
) : PaymentGateway {

    override fun startPayment(
        transactionId: UUID,
        amountInNok: Long,
        phoneNumber: String
    ): PaymentInfo {

        val body = VippsCreatePaymentRequest(
            amount = Amount(currency = "NOK", value = amountInNok * 100), // øre
            paymentMethod = PaymentMethod(type = "WALLET"),
            customer = Customer(phoneNumber = phoneNumber),
            reference = transactionId.toString(),
            returnUrl = "${config.returnUrl}?tx=$transactionId",
            userFlow = "WEB_REDIRECT",
            paymentDescription = "LPG fylling"
        )

        val token = tokenService.getToken()

        val response = webClient.post()
            .uri("${config.baseUrl}/epayment/v1/payments")
            .header("Authorization", "Bearer $token")
            .header("Ocp-Apim-Subscription-Key", config.subscriptionKey)
            .header("Merchant-Serial-Number", config.merchantSerialNumber)
            .header("Idempotency-Key", transactionId.toString())
            .bodyValue(body)
            .retrieve()
            .bodyToMono<VippsCreatePaymentResponse>()
            .block()!!

        paymentsRepository.save(
            PaymentEntity(
                id = transactionId,
                vippsPaymentId = response.paymentId,
                status = PaymentStatus.PENDING
            )
        )

        return PaymentInfo(
            id = transactionId,
            status = PaymentStatus.PENDING,
            redirectUrl = response.redirectUrl
        )
    }

    override fun getPaymentStatus(transactionId: UUID): PaymentStatus {
        // enten slå opp i DB (oppdatert via webhook),
        // eller poll Vipps /epayment/v1/payments/{reference}
    }
}

2.2. Webhook-mottaker i Azure

Lag en liten controller i sky-delen:

@RestController
@RequestMapping("/api/vipps/webhooks")
class VippsWebhookController(
private val paymentsRepository: PaymentsRepository,
private val syncQueue: AzureSyncQueue
) {

    @PostMapping
    fun handleWebhook(@RequestBody event: VippsPaymentEvent): ResponseEntity<Void> {
        // verifiser signatur/secret hvis aktuelt
        paymentsRepository.updateStatus(
            reference = event.reference,
            vippsStatus = event.paymentStatus
        )
        syncQueue.enqueuePaymentStatusChanged(event.reference)
        return ResponseEntity.ok().build()
    }
}

Webhooks registreres mot https://<din-azure-app>/api/vipps/webhooks i Vipps sitt MT-miljø. ￼

2.3. Frontend: React-endringer

Du har allerede pumpesimulatoren – flott! Legg til Vipps-knapp og telefonfelt:
1.	UI-felt:
•	Input for norsk mobilnummer (+47 / 47xxxxxxx).
•	Radioknapper: Kontant / Kort / Vipps.
•	Når Vipps er valgt og bruker trykker “Start”:
•	Kall POST /api/payments/vipps/start.
2.	Stateflow i frontend:

const [paymentStatus, setPaymentStatus] = useState<'IDLE' | 'PENDING' | 'APPROVED' | 'FAILED'>('IDLE');

async function startVippsPayment() {
setPaymentStatus('PENDING');
const res = await fetch(`${API_URL}/payments/vipps/start`, { ... });
const { transactionId } = await res.json();

// Start polling
const interval = setInterval(async () => {
const statusRes = await fetch(`${API_URL}/payments/${transactionId}`);
const { status } = await statusRes.json();
setPaymentStatus(status);
if (status === 'APPROVED' || status === 'FAILED') {
clearInterval(interval);
}
}, 2000);
}

	3.	Kobling mot emulator/pumpe:
	•	Når paymentStatus === 'APPROVED':
	•	Kall backend /api/dispenser/unblock som igjen snakker med EHL-emulatoren / ekte teller.

2.4. Dev-story / profiler

Bruk Spring-profiler for å holde dette ryddig:
•	local:
•	PaymentGateway = SimulatedPaymentGateway
•	Ingen nettverkskall; alt kjører mot emulator.
•	vipps-test:
•	PaymentGateway = VippsPaymentGateway
•	Base-URL = https://apitest.vipps.no
•	Azure-app eksponerer webhook-endpoint.
•	prod:
•	Samme kode, men base-URL = https://api.vipps.no og produksjonsnøkler fra Key Vault.

⸻

3. Forslag til neste konkrete steg for deg
    1.	Lag PaymentGateway-interface og VippsPaymentGateway skeleton som over.
    2.	Legg til Vipps-config i application-local.yml og application-vipps-test.yml.
    3.	Implementer access-token-cache og et minimum av startPayment() (først bare logg request + returner MOCK status).
    4.	Oppdater React-UI med:
          •	Vipps som betalingsvalg
          •	Mobilnummerfelt
          •	Polling av betalingstatus
    5.	Når dere har fått Vipps-testnøkler:
          •	Bytt ut mock med ekte kall til /accesstoken/get og /epayment/v1/payments.
          •	Registrer webhook til Azure-endpointen din.

Da har du en komplett ende-til-ende “Vipps-tapping” som kan demonstreres både med emulatoren og senere mot ekte dispenser – helt i tråd med Vipps sin anbefalte ePayment-flow.