(ns frontend.components.widgets
  (:require [rum.core :as rum]
            [frontend.util :as util]
            [frontend.handler.user :as user-handler]
            [frontend.handler.repo :as repo-handler]
            [frontend.handler.notification :as notification]
            [frontend.handler.web.nfs :as nfs]
            [frontend.state :as state]
            [clojure.string :as string]
            [frontend.ui :as ui]
            [frontend.context.i18n :as i18n]
            [frontend.handler.web.nfs :as nfs]))

(rum/defc choose-preferred-format
  []
  (rum/with-context [[t] i18n/*tongue-context*]
    [:div
     [:h1.title {:style {:margin-bottom "0.25rem"}}
      (t :format/preferred-mode)]

     [:div.mt-4.ml-1
      (ui/button
        "Markdown"
        :on-click
        #(user-handler/set-preferred-format! :markdown))

      [:span.ml-2.mr-2 "-OR-"]

      (ui/button
        "Org Mode"
        :on-click
        #(user-handler/set-preferred-format! :org))]]))

(rum/defcs add-github-repo <
  (rum/local "" ::repo)
  [state]
  (let [repo (get state ::repo)]
    (rum/with-context [[t] i18n/*tongue-context*]
      [:div.p-8.flex.flex-col
       [:div.w-full.mx-auto
        [:div
         [:div
          [:h1.title
           (t :git/add-repo-prompt)]
          [:div.mt-4.mb-4.relative.rounded-md.shadow-sm.max-w-xs
           [:input#repo.form-input.block.w-full.sm:text-sm.sm:leading-5
            {:autoFocus true
             :placeholder "https://github.com/username/repo"
             :on-change (fn [e]
                          (reset! repo (util/evalue e)))}]]]]

        (ui/button
          (t :git/add-repo-prompt-confirm)
          :on-click
          (fn []
            (let [repo (util/lowercase-first @repo)]
              (if (util/starts-with? repo "https://github.com/")
                (let [repo (string/replace repo ".git" "")]
                  (repo-handler/create-repo! repo))

                (notification/show!
                  [:p "Please input a valid repo url, e.g. https://github.com/username/repo"]
                  :error
                  false)))))]])))

(rum/defcs add-local-directory
  []
  (rum/with-context [[t] i18n/*tongue-context*]
    [:div.p-8.flex.flex-col
     [:h1.title "Add Local directory"]
     (let [nfs-supported? (nfs/supported?)]
       [:div.cp__widgets-open-local-directory
        [:div.select-file-wrap.cursor
         (when nfs-supported?
           {:on-click nfs/ls-dir-files})
         [:div
          [:h1.title "Open a local directory"]
          [:p.text-sm
           "Your data will be stored only in your device."]
          (when-not nfs-supported?
            (ui/admonition :warning
                           [:p "It seems that your browser doesn't support the "

                            [:a {:href "https://web.dev/file-system-access/"
                                 :target "_blank"}
                             "new native filesystem API"]
                            [:span ", please use any chromium 86+ browser like Chrome, Vivaldi, Edge, Brave, etc."]]))]]])]))

(rum/defcs add-graph <
  [state & {:keys [graph-types]
            :or {graph-types [:local :github]}
            :as opts}]
  (let [github-authed? (:github-authed? (state/get-me))
        generate-f (fn [x]
                     (case x
                       :github
                       (when github-authed?
                         (rum/with-key (add-github-repo)
                                       "add-github-repo"))

                       :local
                       (rum/with-key (add-local-directory)
                                     "add-local-directory")

                       nil))
        available-graph (->> (set graph-types)
                             (keep generate-f)
                             (vec))]
    (rum/with-context [[t] i18n/*tongue-context*]
      [:div.p-8.flex.flex-col available-graph])))
