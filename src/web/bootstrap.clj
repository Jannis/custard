(ns web.bootstrap
  (:require [clojure.string :as str]))

(def tags '[Accordion
            Affix
            Alert
            Badge
            Breadcrumb
            BreadcrumbItem
            Button
            ButtonGroup
            ButtonInput
            ButtonToolbar
            Carousel
            CarouselItem
            Col
            Collapse
            CollapsibleNav
            Dropdown
            DropdownButton
            DropdownMenu
            DropdownToggle
            Fade
            FormGroup
            Glyphicon
            Grid
            Image
            Input
            InputBase
            Interpolate
            Jumbotron
            Label
            ListGroup
            ListGroupItem
            MenuItem
            Modal
            ModalBody
            ModalDialog
            ModalFooter
            ModalHeader
            ModalTitle
            Nav
            NavBrand
            NavDropdown
            NavItem
            Navbar
            Overlay
            OverlayTrigger
            PageHeader
            PageItem
            Pager
            Pagination
            PaginationButton
            Panel
            PanelGroup
            Popover
            ProgressBar
            ResponsiveEmbed
            Row
            SafeAnchor
            SplitButton
            SplitToggle
            SubNav
            Tab
            Table
            Tabs
            Thumbnail
            Tooltip
            Well])

(defn kebab-case [s]
  (str/join "-" (map str/lower-case (re-seq #"\w[a-z]+" s))))

(defn ^:private gen-react-bootstrap-inline-fn [tag]
  `(defmacro ~(symbol (kebab-case (name tag))) [opts# & children#]
     `(~'~(symbol "js" (str "ReactBootstrap." (name tag)))
       ~opts#
       ~@(map (fn [x#] `(force-children ~x#)) children#))))

(defmacro ^:private gen-react-bootstrap-inline-fns []
 `(do
    ~@(map gen-react-bootstrap-inline-fn tags)))

(gen-react-bootstrap-inline-fns)

(defn ^:private gen-react-bootstrap-fn [tag]
  `(defn ~(symbol (kebab-case (name tag))) [opts# & children#]
     (js/React.createElement
      ~(symbol "js" (str "ReactBootstrap." (name tag)))
      opts#
      (map force-children children#))))

(defmacro ^:private gen-react-bootstrap-fns []
  `(do
     ~@(map gen-react-bootstrap-fn tags)))
