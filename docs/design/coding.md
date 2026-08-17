# Coding 

One of the desired features for Scaffold is the ability to assess the Blooms Taxonomy level of each question.

This is a specific instance of "coding" the question according to a code book.  

There could be other ways of coding a question as well, including:

* assigning it to the concept or subconcept that it most pertains to
* assigning it a level in other taxonomies

One difference between "coding" and "tagging" is that coding proceeds according to the conventions of Content Analysis, where 
there are:

* coding rounds on random samples, done by independent coders
* calculation of inter-rater reliability (Cohen's Kappa for precisely two raters, Krippendorf's Alpha for two, three or more)
* a process of resolving differences and refining the codebook (this is typically a single integrated process)

It would be helpful if the software itself assisted with managing this process.  That means we need the following:

# A way to Manage Codebooks

* Code books have codes, and explanations
* Codes are typically mutually exclusive; for simplicity, we'll assume mutual exclusivity of codes for this application
* Code books are versioned; they stay consistent for a specific round, but are typically revised during the resolution of differences

# A way to pull random samples

* A random sample needs to be truly random across a set of PrairieLearn questions.
* It then needs to be stored so that it is stable while raters go through a round, and archived along with the results.
* PrairieLearn questions can change, so some metadata such as the date, github commit, etc. for each question may be helpful to store.

# A way for raters to assign ratings from a code book to a PrairieLearn Question

* We need to be able to assign ratings in a straightforward way, and keep track of progress through a round.
* Individual raters should be able to see their progress.
* The entire team should be able to see the progress of each rater, without actually seeing the ratings of other raters
  (until it's time for the resolution of differences).

# There should also be a way to assign final ratings to artifacts once coders have been calibrated.

* Once IRR is established, then coders can independently code the entire corpus.  
* It is helpful to assign coders to the "next uncoded item" randomly, and track who coded each item.
* It is perhaps also helpful to have an "audit" process where coders are assigned randomly to recode items coded by others, 
  and calculated IRR to ensure that it not only *starts* high, but *stays* high, and to flag cases where there are disagreements,
  so those can be reviewed.

Ultimately, the goal is to have:
* the Blooms Taxonomy level of every PrairieLearn question assessed in a way that is rigorous and reliable
* have a process for keeping this evergreen as new questions are added
* have a process for highlighting where there are gaps in terms of assessing learning objectives at multiple levels.

Note: currently we have "concepts" and "subconcepts" but we don't have a concept of "learning objective" in the Scaffold.

Is that a gap we should consider addressing?

