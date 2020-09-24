(ns hier-set.core
  "Provides a 'hierarchical set' data structure.  See `hier-set` for details."
  (:refer-clojure :exclude [descendants ancestors])
  (:import [java.util Set])
  (:import [clojure.lang IFn ILookup IObj IPersistentCollection IPersistentSet
                         PersistentTreeSet Seqable Sorted]))

(defprotocol Hierarchical
  "Operations on collections defining hierarchical relationships."
  (ancestors [coll key] [coll key strict?]
    "Return a lazy sequence of all ancestors of `key` in `coll`.  Do not
include `key` if `strict?` is true, defaulting to false.")
  (descendants [coll key] [coll key strict?]
    "Return a lazy sequence of all descendants of `key` in `coll`.  Do not
include `key` if `strict?` is true, defaulting to false."))

(defn- strictify
  [f coll key strict?]
  (let [result (f coll key)]
    (if (and strict? (= key (first result)))
      (drop 1 result)
      result)))

(deftype HierSet [meta hcontains? ^PersistentTreeSet contents parents]
  ;; meta - the instance's IObj metadata
  ;; hcontains? - the containment predicate function
  ;; contents - the sorted set of the HierSet's primary members
  ;; parents - map of members to their immediate parent members

  Object
  (toString [this] (str contents))
  (hashCode [this] (hash contents))
  (equals [this other]
    (.equals contents other))

  IObj
  (meta [this] meta)
  (withMeta [this meta]
    (HierSet. meta hcontains? contents parents))

  Hierarchical
  (ancestors [this key]
    (letfn [(ancestors-of [k]
              (when k (cons k (lazy-seq (ancestors-of (parents k))))))
            (not-ancestor? [k] (not (hcontains? k key)))]
      (let [sibling (first (rsubseq contents <= key))]
        (->> sibling ancestors-of (drop-while not-ancestor?)))))
  (ancestors [this key strict?]
    (strictify ancestors this key strict?))
  (descendants [this key]
    (take-while #(hcontains? key %) (subseq contents >= key)))
  (descendants [this key strict?]
    (strictify descendants this key strict?))

  Seqable
  (seq [this] (seq contents))

  IPersistentCollection
  (count [this] (count contents))
  (cons [this key]
    (if (contains? contents key)
      this
      (let [parent (first (ancestors this key))
            kids (filter #(= parent (parents %)) (descendants this key))
            parents (reduce #(assoc %1 %2 key) (assoc parents key parent) kids)
            contents (conj contents key)]
        (HierSet. meta hcontains? contents parents))))
  (empty [this]
    (HierSet. meta hcontains? (empty contents) (empty parents)))
  (equiv [this other]
    (.equals this other))

  IPersistentSet
  (disjoin [this key]
    (if-not (contains? contents key)
      this
      (let [parent (parents key), contents (disj contents key)
            kids (filter #(= key (parents %)) (descendants this key)),
            parents (reduce #(assoc %1 %2 parent) (dissoc parents key) kids)]
        (HierSet. meta hcontains? contents parents))))
  (contains [this key]
    (boolean (.get this key)))
  (get [this key]
    (seq (ancestors this key)))

  Set
  (containsAll [this coll] (.containsAll contents coll))
  (isEmpty [this] (.isEmpty contents))
  (iterator [this] (.iterator contents))
  (size [this] (.size contents))
  (^objects toArray [this] (.toArray contents))
  (^objects toArray [this ^objects a] (.toArray contents a))

  Sorted
  (comparator [this] (.comparator contents))
  (entryKey [this entry] entry)
  (seq [this ascending] (.seq contents ascending))
  (seqFrom [this key ascending] (.seqFrom contents key ascending))

  IFn
  (invoke [this key]
    (get this key))
  (invoke [this key not-found]
    (get this key not-found)))

(defn hier-set-by
  "As hier-set, but specifying the comparator to use for element comparison."
  [hcontains? comparator & keys]
  (letfn [(find-parent [[parents ancestors] key]
            (let [not-ancestor? (fn [k] (not (hcontains? k key)))
                  ancestors (drop-while not-ancestor? ancestors)]
              [(assoc parents key (first ancestors)) (cons key ancestors)]))]
    (let [contents (apply sorted-set-by comparator keys)
          parents (first (reduce find-parent [{} ()] contents))]
      (HierSet. nil hcontains? contents parents))))

(defn hier-set
  "Constructs a hierarchical set with the containment predicate `hcontains?`
and primary members `keys`.  The `hcontains?` predicate should be a function of
two arguments over the type of the set elements; it should return `true` if the
first argument contains the second, and false otherwise.

A hierarchical set is a set of elements which may hierarchically contain other
elements.  The hierarchical relationship is defined by the element sort-order
and the `hcontains?` predicate, with the following constraints: (a) elements
must sort prior to any descendants; and (b) elements must contain all elements
which sort between themselves and any descendant.  Note that this means
that `(hcontains? x x)` must be true, and that elements are thus considered to
be both ancestors and descendants of themselves.

Lookup in the set returns a seq of all primary members which are ancestors of
the provided key, or nil if the provided key is not a descendant of any primary
member."
  [hcontains? & keys] (apply hier-set-by hcontains? compare keys))
