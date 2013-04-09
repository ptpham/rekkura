Rekkura 0.0.0: A General Game Playing Framework
===============================================

# Introduction
Rekkura is a lightweight Java GGP framework with a flexible representation of logic. 
If you are new to GGP, please take a look at <http://cs227b.stanford.edu>.
If you are interested in a more mature code base, please refer to the excellent <https://code.google.com/p/ggp-base/>.

# Representation
Instead of making the distinction between variables, propositions, relations, functions, constants, terms and sentences, Rekkura just has 3 logical primitives: Dob, Atom, and Rule. A Dob represents a "discrete object". A Dob is actually a directed acyclic graph such that each node has an ordering over its children. An Atom is simply a Dob that has a truth value attached to it. A Rule consists of a head Atom that is entailed if all of its body Atoms are true. A Rule also has a set of Dobs that it considers "variables" in its scope.

# Auxiliary Data Structures
The most unfamiliar data structure that is potentially useful in the codebase right now is probably the Fortre (Form Tree). This class is responsible for partitioning the kinds of Dobs that exist in some context. One can think of a Dob and a set of variables as defining a set: the set of all ground Dobs that unify against the given Dob. The Fortre organizes Dobs so that if Dob B represents a strict subset of Dob A, then B will be a child of A. The Fortre is also capable of generating generalizations of Dobs. In particular, if a set of Dobs form a connected component through the "symmetrizing" relationship, then their generalization is constructed as a node in the Fortre. Dob A symmetrizes with Dob B if the unification of A with B is successful and each mapping entry in that unification either has a variable as its key or a variable as its value. Two nodes A, B satisfy the symmetrizing relationship if A symmetrizes with B or B symmetrizes with A; the reason for the asymmetry is that unification success is not a symmetric relation.

There are a number of other potentially very useful constructs and algorithms. Please browse the rekkura.logic package and take a look at Ruletta, Terra, Topper, and Unifier. Send me a message in the likely situation that documentation is woefully lacking and/or you find egregious bugs.

# GGP Protocol
There are a few differences a user will need to worry about between the standard interface of a GGP player and the interface presented in Rekkura:
* Players are expected and encouraged to supply a provisional move. When subclass from Player, use the setDecision() method to set the provisional move. Rekkura will send a player's latest decision when the time comes.
* The START phase of in GGP is lumped into the PLAY phase. Player.StateBased implementations will not need to worry about converting to and from the GGP definition of the phases. Simply call isValidState() when subclassing from Player.StateBased to know if you should return as soon as possible from the method you are currently in. Player.StateBased will properly update your state before calling back down to your methods.



