import {Appearance, Stripe, StripeElements, StripePaymentElementOptions} from '@stripe/stripe-js';

interface Input {
  studentId: string
  numberOfLessons: number
  lengthOfLesson: number
}

window.onload = async function() {

  const numberOfLessonsEl: HTMLInputElement | null = document.querySelector("#number-of-lessons")
  const lengthOfLessonEl: HTMLInputElement | null = document.querySelector("#length-of-lesson")
  const studentIdEl: HTMLInputElement | null = document.querySelector("#student-id")
  const emailEl: HTMLInputElement | null = document.querySelector("#email")
  const csrfInput: HTMLInputElement | null = document.querySelector("input[name='csrfToken']")
  const apiKeyEl: HTMLInputElement | null = document.querySelector("#api-key")


  if(csrfInput && apiKeyEl && numberOfLessonsEl && lengthOfLessonEl && studentIdEl && emailEl) {
    const payments = new Payments(emailEl.value)
    payments.setLoading(true)
    const stripe = window.Stripe(apiKeyEl.value)
    const numberOfLessons = Number.parseInt(numberOfLessonsEl.value, 10)
    const lengthOfLesson = Number.parseInt(lengthOfLessonEl.value, 10)
    const studentId = studentIdEl.value
    const elements = await payments.initialise(stripe, {studentId, numberOfLessons, lengthOfLesson}, csrfInput.value);
    await payments.checkStatus(stripe);
    await payments.setSubmitHandler(stripe, elements)
    payments.setLoading(false)
  }
}

export class Payments {
  emailAddress: string

  constructor(email: string) {
    this.emailAddress = email
  }
  async setSubmitHandler(stripe: Pick<Stripe, "confirmPayment">, elements: StripeElements) {
    const form: HTMLFormElement | null = document.querySelector("#payment-form")
    if(form) {
      await form.addEventListener("submit", async ev => {
        ev.preventDefault();
        this.setLoading(true);

        const { error } = await stripe.confirmPayment({
          elements,
          confirmParams: {
            return_url: `${window.location.protocol}://${window.location.host}/payment-confirmation`,
            receipt_email: this.emailAddress,
          },
        });
        if (error.message && (error.type === "card_error" || error.type === "validation_error")) {
          this.showMessage(error.message);
        } else {
          this.showMessage("An unexpected error occurred.");
        }

        this.setLoading(false);
      });
    }
  }

  async initialise(stripe: Pick<Stripe, "elements">, input: Input, csrfValue: string): Promise<StripeElements> {

    const response = await fetch("/payment-intent", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Csrf-Token" : csrfValue
      },
      credentials: "include",
      body: JSON.stringify(input),
    });
    const { clientSecret } = await response.json();

    const appearance: Appearance = {
      theme: 'stripe',
    };

    const elements = stripe.elements({ appearance, clientSecret });

    const linkAuthenticationElement = elements.create("linkAuthentication");
    linkAuthenticationElement.mount("#link-authentication-element");

    linkAuthenticationElement.on('change', (event) => {
      this.emailAddress = event.value.email;
    });

    const paymentElementOptions: StripePaymentElementOptions = {
      layout: "tabs",
      defaultValues : {
        billingDetails: {
          email: this.emailAddress
        }
      }
    };

    const paymentElement = elements.create("payment", paymentElementOptions);
    paymentElement.mount("#payment-element");
    return elements
  }

// Fetches the payment intent status after payment submission
  async checkStatus(stripe: Pick<Stripe, "retrievePaymentIntent">) {
    const clientSecret = new URLSearchParams(window.location.search).get(
      "payment_intent_client_secret"
    );

    if (!clientSecret) {
      return;
    }

    const { paymentIntent } = await stripe.retrievePaymentIntent(clientSecret);

    if(paymentIntent) {
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
  }

  showMessage(messageText: string) {
    const messageContainer: HTMLElement | null = document.querySelector("#payment-message");
    if(messageContainer) {
      messageContainer.classList.remove("hidden");
      messageContainer.textContent = messageText;

      setTimeout(function () {
        messageContainer.classList.add("hidden");
        messageText = "";
      }, 4000);
    }

  }

// Show a spinner on payment submission
  setLoading(isLoading: boolean) {
    const submit: HTMLButtonElement | null = document.querySelector("#submit")
    const spinner: HTMLDivElement | null = document.querySelector(".lds-roller")
    if(submit && spinner) {
      if (isLoading) {
        // Disable the button and show a spinner
        submit.disabled = true;
        spinner.classList.remove("hidden");
      } else {
        submit.disabled = false;
        spinner.classList.add("hidden");
      }
    }

  }

}
