(ns com.htm2.middleware
  (:require [com.biffweb :as biff]
            [muuntaja.middleware :as muuntaja]
            [ring.middleware.anti-forgery :as csrf]
            [ring.middleware.defaults :as rd]
            [ring.middleware.params :refer [wrap-params]]
            [ring.middleware.oauth2 :refer [wrap-oauth2]]))

(defn wrap-redirect-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (if (some? (:user-email session))
      {:status 303
       :headers {"location" "/app"}}
      (handler ctx))))

(defn wrap-signed-in [handler]
  (fn [{:keys [session] :as ctx}]
    (biff/pprint (:user-email session))
    (if (some? (:user-email session))
      (handler ctx)
      {:status 303
       :headers {"location" "/signin?error=not-signed-in"}})))

;; Stick this function somewhere in your middleware stack below if you want to
;; inspect what things look like before/after certain middleware fns run.
(defn wrap-debug [handler]
  (fn [ctx]
    (let [response (handler ctx)]
      (println "REQUEST")
      (biff/pprint ctx)
      (def ctx* ctx)
      (println "RESPONSE")
       (biff/pprint response)
      (def response* response)
      response)))

(defn get-oauth-config [{:keys [biff/secret] :as ctx}]
  {:google
    {:authorize-uri    (:oauth2/google-authorize-uri ctx)
     :access-token-uri (:oauth2/google-access-token-uri ctx)
     :client-id        (:oauth2/google-client-id ctx)
     :client-secret    (secret :oauth2/google-client-secret)
     :scopes           (:oauth2/google-scopes ctx)
     :launch-uri       (:oauth2/google-launch-uri ctx)
     :redirect-uri     (:oauth2/google-redirect-uri ctx)
     :landing-uri      (:oauth2/google-landing-url ctx)}})

(defn my-wrap-oauth2 [base-handler]
  (fn [ctx]
    (let [config (get-oauth-config ctx)
          handler (wrap-oauth2 base-handler config)]
      (handler ctx))))

(defn wrap-site-defaults [handler]
  (-> handler
      biff/wrap-render-rum
      biff/wrap-anti-forgery-websockets
      csrf/wrap-anti-forgery
      ;; I can't put it here because I don't get how to assign routes correctly
      ;; my-wrap-oauth2
      biff/wrap-session
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults (-> rd/site-defaults
                            (assoc-in [:security :anti-forgery] false)
                            (assoc-in [:responses :absolute-redirects] true)
                            (assoc :session false)
                            (assoc :static false)))))

(defn wrap-api-defaults [handler]
  (-> handler
      muuntaja/wrap-params
      muuntaja/wrap-format
      (rd/wrap-defaults rd/api-defaults)))

(defn wrap-base-defaults [handler]
  (-> handler
      my-wrap-oauth2
      wrap-params
      biff/wrap-session
      muuntaja/wrap-params
      biff/wrap-https-scheme
      biff/wrap-resource
      biff/wrap-internal-error
      biff/wrap-ssl
      biff/wrap-log-requests
))
