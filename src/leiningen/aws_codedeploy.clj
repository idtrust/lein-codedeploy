(ns leiningen.aws-codedeploy
   "Plugin to create a code deploy package"
   (:require [clj-yaml.core :as yaml]
             [clojure.pprint :as pp]
             [stencil.core :as st])
   (:import [java.io File
                     FileInputStream
                     FileOutputStream
                     InputStream
                     IOException
                     OutputStream]
            [java.util.zip ZipEntry
                           ZipOutputStream]
            [org.apache.commons.io FileUtils IOUtils]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- slurp-yml-file
   [filename]
   (when-not filename
      (throw (IllegalArgumentException.
               (format "yml file=%s needs to be defined", filename))))
   (slurp filename))

(defn- replace-tokens
   [yml-string tokenmap]
   (when tokenmap
      (st/render-string yml-string tokenmap)))

(defn trim-prefix-slash
   [s]
   (.replaceAll ^String s "^/", ""))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- get-files
   "Retrieve the set of files from the appspec"
   [appspec]
   (let [src-files (map #(:source %) (:files appspec))
         hook-files (mapcat (fn [e] (map #(:location %) e))
                            (vals (:hooks appspec)))]
      [(map trim-prefix-slash src-files)
       hook-files]))

(defn- add-file-to-zip
   [^ZipOutputStream zos ^File f]
   (with-open [fis (FileInputStream. f)]
      (let [p (.getPath f)]
         (println "Archiving file: " p)
         (.putNextEntry zos (ZipEntry. p))
         (IOUtils/copy ^InputStream fis ^OutputStream zos)
         (.closeEntry zos))))

(defn- add-script-to-zip
   [^ZipOutputStream zos script-file tokenmap]
   (println script-file)
   (let [script (slurp script-file)
         script (replace-tokens script tokenmap)
          _ (println (format "Script :%s" script))]
      (println "Archiving script-file: " script-file)
      (.putNextEntry zos (ZipEntry. script-file))
      (.write zos (.getBytes script))
      (.closeEntry zos)))

(defn- get-file-objects
   [filename]
   (let [f (File. filename)]
      (if (.isDirectory f)
         (FileUtils/listFiles f nil true)
         [f])))

(defn- mk-zip-archive
   [archive appspec content-files script-files tokenmap]
   (with-open [fos (FileOutputStream. archive)
               zos (ZipOutputStream. fos)]

      ;; Write the appspec file
      (.putNextEntry zos (ZipEntry. "appspec.yml"))
      (.write zos (-> appspec yaml/generate-string .getBytes))
      (.closeEntry zos)
      (doseq [sf script-files]
         (add-script-to-zip zos sf tokenmap))
      ;; Write content files
      (doseq [f content-files]
         (doseq [entry (get-file-objects f)]
            (add-file-to-zip zos entry)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn- mk-codedeploy-archive
   [project]
   (let [spec (:appspec project)
         package (or (:package spec) "package")
         version (:version project)
         tokenmap (assoc (:tokens spec) :version version)
         appspec (-> (:appspec-file spec)
                     slurp-yml-file
                     (replace-tokens tokenmap)
                     yaml/parse-string)
         [content-files script-files] (get-files appspec)
         zip-archive (mk-zip-archive (format "%s-%s.zip" package version)
                                     appspec
                                     content-files
                                     script-files
                                     tokenmap)]
      (pp/pprint appspec)
      (pp/pprint content-files)
      (pp/pprint script-files)))

(defn aws-codedeploy
  [project & args]
  (mk-codedeploy-archive project))
