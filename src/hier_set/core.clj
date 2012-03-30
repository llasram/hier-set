(ns hier-set.core
  "Provides a 'hierarchical set' data structure.  See `hier-set` for details."
  (:refer-clojure :exclude [descendants ancestors])
  (:import [clojure.lang IFn ILookup IObj Seqable Sorted]))

(defprotocol Hierarchical
  "Operations on collections defining hierarchical relationships."
  (ancestors [coll key]
    "Return a lazy sequence of all ancestors of `key` in `coll`.")
  (descendants [coll key]
    "Return a lazy sequence of all descendants of `key` in `coll`."))

(deftype HierSet [meta contains? ^Sorted contents parents]
  ;; meta - the instance's IObj metadata
  ;; contains? - the containment predicate function
  ;; contents - the sorted set of the HierSet's primary members
  ;; parents - map of members to their immediate parent members

  Object
  (toString [this]
    (str contents))

  IObj
  (meta [this] meta)
  (withMeta [this meta]
    (HierSet. meta contains? contents parents))

  Hierarchical
  (ancestors [this key]
    (let [sibling (first (rsubseq contents <= key))
          ancestors-of (fn ancestors-of [k]
                         (when k (cons k (lazy-seq (ancestors-of (parents k))))))
          not-ancestor? (fn [k] (not (contains? k key)))]
      (->> (ancestors-of sibling) (drop-while not-ancestor?))))
  (descendants [this key]
    (take-while #(contains? key %) (subseq contents >= key)))

  ILookup
  (valAt [this key]
    (seq (ancestors this key)))
  (valAt [this key not-found]
    (or (.valAt this key) not-found))

  IFn
  (invoke [this key]
    (get this key))
  (invoke [this key not-found]
    (get this key not-found))

  Seqable
  (seq [this] (seq contents))

  Sorted
  (comparator [this] (.comparator contents))
  (entryKey [this entry] entry)
  (seq [this ascending] (.seq contents ascending))
  (seqFrom [this key ascending] (.seqFrom contents key ascending)))

(defn hier-set-by
  "As hier-set, but specifying the comparator to use for element comparison."
  [contains? comparator & keys]
  (letfn [(find-parent [[parents ancestors] key]
            (let [not-ancestor? (fn [k] (not (contains? k key)))
                  ancestors (drop-while not-ancestor? ancestors)]
              [(assoc parents key (first ancestors)) (cons key ancestors)]))]
    (let [contents (apply sorted-set-by comparator keys)
          parents (first (reduce find-parent [{} ()] contents))]
      (HierSet. nil contains? contents parents))))

(defn hier-set
  "Constructs a hierarchical set with the containment predicate `contains?` and
primary members `keys`.  The `contains?` predicate should be a function of two
arguments over the type of the set elements; it should return `true` if the
first argument contains the second, and false otherwise.

A hierarchical set is a set of elements which may hierarchically contain other
elements.  The hierarchical relationship is defined by the element sort-order
and the `contains?` predicate, with the following constraints: (a) elements
must sort prior to any descendants; and (b) elements must contain all elements
which sort between themselves and any descendant.  Note that this means
that `(contains? x x)` must be true, and that elements are thus considered to
be both ancestors and descendants of themselves.

Lookup in the set returns a seq of all primary members which are ancestors of
the provided key, or nil if the provided key is not a descendant of any primary
member."
  [contains? & keys] (apply hier-set-by contains? compare keys))
