'use strict';

/******************************************************************************
Copyright (c) Microsoft Corporation.

Permission to use, copy, modify, and/or distribute this software for any
purpose with or without fee is hereby granted.

THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES WITH
REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY SPECIAL, DIRECT,
INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING FROM
LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT, NEGLIGENCE OR
OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION WITH THE USE OR
PERFORMANCE OF THIS SOFTWARE.
***************************************************************************** */

function __awaiter(thisArg, _arguments, P, generator) {
    function adopt(value) { return value instanceof P ? value : new P(function (resolve) { resolve(value); }); }
    return new (P || (P = Promise))(function (resolve, reject) {
        function fulfilled(value) { try { step(generator.next(value)); } catch (e) { reject(e); } }
        function rejected(value) { try { step(generator["throw"](value)); } catch (e) { reject(e); } }
        function step(result) { result.done ? resolve(result.value) : adopt(result.value).then(fulfilled, rejected); }
        step((generator = generator.apply(thisArg, _arguments || [])).next());
    });
}

const items = [{ id: "xl-tshirt" }];
window.onload = function () {
    return __awaiter(this, void 0, void 0, function* () {
        const csrfInput = document.querySelector("input[name='csrfToken']");
        const apiKeyEl = document.querySelector("#api-key");
        const payments = new Payments();
        payments.setLoading(true);
        if (csrfInput && apiKeyEl) {
            const stripe = window.Stripe(apiKeyEl.value);
            const elements = yield payments.initialise(stripe, csrfInput.value);
            yield payments.checkStatus(stripe);
            yield payments.setSubmitHandler(stripe, elements);
            payments.setLoading(false);
        }
    });
};
class Payments {
    constructor() {
        this.emailAddress = '';
    }
    setSubmitHandler(stripe, elements) {
        return __awaiter(this, void 0, void 0, function* () {
            const form = document.querySelector("#payment-form");
            if (form) {
                yield form.addEventListener("submit", (ev) => __awaiter(this, void 0, void 0, function* () {
                    ev.preventDefault();
                    this.setLoading(true);
                    const { error } = yield stripe.confirmPayment({
                        elements,
                        confirmParams: {
                            return_url: "http://localhost:9000/payment-confirmation",
                            receipt_email: this.emailAddress,
                        },
                    });
                    if (error.message && (error.type === "card_error" || error.type === "validation_error")) {
                        this.showMessage(error.message);
                    }
                    else {
                        this.showMessage("An unexpected error occurred.");
                    }
                    this.setLoading(false);
                }));
            }
        });
    }
    initialise(stripe, csrfValue) {
        return __awaiter(this, void 0, void 0, function* () {
            const response = yield fetch("/payment-intent", {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    "Csrf-Token": csrfValue
                },
                credentials: "include",
                body: JSON.stringify({ items }),
            });
            const { clientSecret } = yield response.json();
            const appearance = {
                theme: 'stripe',
            };
            const elements = stripe.elements({ appearance, clientSecret });
            const linkAuthenticationElement = elements.create("linkAuthentication");
            linkAuthenticationElement.mount("#link-authentication-element");
            linkAuthenticationElement.on('change', (event) => {
                this.emailAddress = event.value.email;
            });
            const paymentElementOptions = {
                layout: "tabs",
            };
            const paymentElement = elements.create("payment", paymentElementOptions);
            paymentElement.mount("#payment-element");
            return elements;
        });
    }
    // Fetches the payment intent status after payment submission
    checkStatus(stripe) {
        return __awaiter(this, void 0, void 0, function* () {
            const clientSecret = new URLSearchParams(window.location.search).get("payment_intent_client_secret");
            if (!clientSecret) {
                return;
            }
            const { paymentIntent } = yield stripe.retrievePaymentIntent(clientSecret);
            if (paymentIntent) {
                switch (paymentIntent.status) {
                    case "succeeded":
                        this.showMessage("Payment succeeded!");
                        break;
                    case "processing":
                        this.showMessage("Your payment is processing.");
                        break;
                    case "requires_payment_method":
                        this.showMessage("Your payment was not successful, please try again.");
                        break;
                    default:
                        this.showMessage("Something went wrong.");
                        break;
                }
            }
        });
    }
    showMessage(messageText) {
        const messageContainer = document.querySelector("#payment-message");
        if (messageContainer) {
            messageContainer.classList.remove("hidden");
            messageContainer.textContent = messageText;
            setTimeout(function () {
                messageContainer.classList.add("hidden");
                messageText = "";
            }, 4000);
        }
    }
    // Show a spinner on payment submission
    setLoading(isLoading) {
        const submit = document.querySelector("#submit");
        const spinner = document.querySelector(".lds-roller");
        if (submit && spinner) {
            if (isLoading) {
                // Disable the button and show a spinner
                submit.disabled = true;
                spinner.classList.remove("hidden");
            }
            else {
                submit.disabled = false;
                spinner.classList.add("hidden");
            }
        }
    }
}

exports.Payments = Payments;
