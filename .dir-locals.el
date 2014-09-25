;;; Directory Local Variables
;;; See Info node `(emacs) Directory Variables' for more information.

((clojure-mode
  (eval define-clojure-indent
		(match 'defun)))
 (html-mode
  (tab-width . 2)
  (sgml-basic-offset . 2))
 (less-css-mode
  (tab-width . 2)
  (css-indent-offset . 2)))
