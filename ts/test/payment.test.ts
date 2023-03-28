import {Input, Payments} from "../src/payment";
import {Stripe, StripeElements} from "@stripe/stripe-js";
import fetchMock, {enableFetchMocks} from "jest-fetch-mock"

enableFetchMocks()

describe('payment', () => {
  test('initialise calls the correct stripe functions', async () => {
    fetchMock.mockResponse(JSON.stringify({clientSecret: "test-secret"}))
    const elementsFn = jest.fn()
    const createFn = jest.fn()
    const mount = jest.fn()
    const stripe: Pick<Stripe, "elements"> = {
      elements: elementsFn.mockImplementation(arg => ({
        create: createFn.mockImplementation(args => ({
          mount,
          on: jest.fn().mockImplementation(cb => {
          })
        }))
      }))
    }
    const input: Input = {
      lengthOfLesson: 30,
      numberOfLessons: 1,
      studentId: "id"
    }
    const payments = new Payments("test@test.com")
    await payments.initialise(stripe, input, "csrf")
    const appearance = {theme: 'stripe'}
    expect(elementsFn).toHaveBeenCalledWith({appearance, clientSecret: "test-secret"})
    expect(createFn).toHaveBeenCalledWith("linkAuthentication")
    expect(createFn).toHaveBeenCalledWith("payment", {
      layout: "tabs",
      defaultValues : {
        billingDetails: {
          email: "test@test.com"
        }
      }
    })
    expect(mount).toHaveBeenCalledWith("#link-authentication-element")
  });

  test("setSubmitHandler should confirm the payment", async () => {
    const form = document.createElement("form")
    form.setAttribute("id", "payment-form")
    document.body.appendChild(form)
    const confirmPayment = jest.fn().mockImplementation(_ => ({error: {message: "error", type: "card_error"}}))
    const stripe: Pick<Stripe,"confirmPayment"> = {
      confirmPayment
    }
    const elements: StripeElements = {
      create: jest.fn(),
      fetchUpdates: jest.fn(),
      getElement: jest.fn(),
      update: jest.fn()
    }
    const setLoading = jest.fn()
    const showMessage = jest.fn()
    const payments = new Payments("test@test.com")
    payments.setLoading = setLoading
    payments.showMessage = showMessage

    await payments.setSubmitHandler(stripe, elements)
    await form.submit()

    expect(confirmPayment).toHaveBeenCalled()
    expect(setLoading).toHaveBeenCalledWith(true)
    expect(setLoading).toHaveBeenCalledWith(false)
    expect(showMessage).toHaveBeenCalledWith("error")
  })

  const cases = [
    ["succeeded", "Payment succeeded!"],
    ["processing", "Your payment is processing."],
    ["requires_payment_method", "Your payment was not successful, please try again."],
    ["another_status", "Something went wrong."]
  ]

  test.each(cases)("checkStatus should show a message for status %s", async (status, message) => {
    jest.spyOn(URLSearchParams.prototype, "get").mockReturnValue("client-secret")

    const paymentIntent = {status: status}
    const retrievePaymentIntent = jest.fn().mockImplementation(_ => new Promise((res, _) => res({paymentIntent})))
    const stripe: Pick<Stripe,"retrievePaymentIntent"> = {
      retrievePaymentIntent
    }
    const showMessage = jest.fn()
    const payments = new Payments("test@test.com")
    payments.showMessage = showMessage

    await payments.checkStatus(stripe)
    expect(retrievePaymentIntent).toHaveBeenCalledWith("client-secret")
    expect(showMessage).toHaveBeenCalledWith(message)
  })
});
