# hier-set

Library providing a "hierarchical set" data structure.  The provided data
structure is set of elements with a defined hierarchical relationship.  An
element is considered to be in the set either if it is provided as a primary
set member or if it is a descendant of any such element.  Lookup in the set not
only determines set membership, but also finds all primary set members which
are ancestors of the lookup element.

The hierarchical relationship is defined by the element sort-order and a
separate containment predicate, with the following constraints:

* elements must sort prior to any descendants; and
* elements must contain all elements which sort between themselves and any
  descendant.

This is sufficient to represent simple hierarchical systems where the hierarchy
is implicit in the entities involved, such as the Java package system,
hierarchical filesystems, or IP networks.  It is inappropriate for modeling
complex, ad hoc hierarchies, such as the relationships between classes with
multiple inheritance.

## Usage

Add `hier-set` to the `:dependencies` list in your
[Leiningen](https://github.com/technomancy/leiningen) `project.clj`:

```clj
[hier-set "1.1.2"]
```

Primary usage is then through the `hier-set` and `hier-set-by` constructor
functions in the `hier-set.core` namespace.  In addition to set lookup as
described above, the `hier-set.core/ancestors` and `hier-set.core/descendants`
functions also provide access to lazy sequences of the ancestors and
descendants respectively of a provided key.

## Example

A trivial example:

```clj
(ns example.hier-set
  (:require [hier-set.core :as hs])
  (:use [hier-set.core :only [hier-set]])

(def with-starts? #(.startsWith %2 %1))

(def hs (hier-set with-starts? "ack" "foo" "foo.bar" "quux")

(get hs "bar")              ;;=> nil
(get hs "foo")              ;;=> ("foo")
(get hs "foo.bar.baz")      ;;=> ("foo.bar" "foo")
(hs/ancestors hs "bar")     ;;=> ()
(hs/ancestors hs "foo.baz") ;;=> ("foo")
(hs/descendants hs "foo")   ;;=> ("foo" "foo.bar")
```

## License

Copyright © 2012, 2014 Marshall Bockrath-Vandegrift.

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
