# PrairieLearn Interface

## Functional needs

Here is a list of the ways Scaffold needs to be able to interface with PrairieLearn, separated from the mechanism we will use to 
acheive the interface.

| Need | Pull (from PL) // or Push (to PL) | Expalanation |
|-|-|-|
| For a given course instance, get all assessments | Pull | We want to be able to give the instructor the ability to select which assessments appear in the Assessment dropdown so that students can get help with selected PrairieLearn assessments on the concept graph. |
| For a given course assesment, get all of the questions on that assessment | Pull | We want to give the instructor able to link questions to specific concepts or subconcepts on the concept graph, and students to be able to select a questions from an assessment and link it to a concept |
| For a given course, we want to be able to get all questions | Pull | We want to be able to give the instructor the ability to link a concept or subconcept to one or more PL questions that students can use to test their understanding of the concept |
| For a given question, we want to be able to create a "Scaffold Assessment", a special assessment that exists solely for the purpose of allowing students to practice with a concept they found on the concept graph | Push | We want students to be able to access the questions that connect with a particular concept so that they can practice | 


## Mechanisms

There are two mechanisms that are available currently for interacting with PL

1. Github API through classic PATs.  
2. PrairieLearn API through PrairieLearn PAT.

In both cases, these carry the user rights and permissions (on Github repos and/or PrairieLearn) of the user that sets up the PAT, so these are set up per Admin/Instructor user.

## Capabilities/Limitations of each API

Reading from the Github API can:

* Given the name of a course repo, list all course instances by name.  And for each, it can get this information from the `infoCourseInstance.json` file:
  ```
  {
    "uuid": "eb2647dc-9e3d-46cf-b365-b952dbe617e4",
    "longName": "Spring 2026",
    "allowAccess": [
      {
        "role": "Student",
        "startDate": "2026-03-29T08:00:00",
        "endDate": "2026-06-30T23:59:59"
      }
    ]
  }
  ```
  Crucially, it cannot however, give us the course instance id. 
  
* Given the name of a course repo, list all questions for a course by their alphanumeric questionId.  For each question, we can get this information from `info.json`, as well as any other fields that
  appear in `info.json`; see [PL documentation for info.json](https://docs.prairielearn.com/question/overview/#metadata-infojson)
  ```
  {
    "uuid": "b970695b-4f08-471f-b3d2-d7d12933f395",
    "title": "Hypothesis Testing (mean, randomized)",
    "topic": "Default",
    "type": "v3"
  }
  ```
  
* Given a course repo, and the name of a course instance, list all assessments by their alphanumeric name.  For each
  assessment, what we can get is the information in the `infoAsssessment.json`, documented [here](https://docs.prairielearn.com/assessment/configuration/) which
  includes a list of the alphanumeric questionId for each question included.

  But we cannot get the numeric assesment id, or the student facing URL for the assessment. 

Using the Github API with write access, we can create a new assessment for a question or set of questions.  The intention here is to set up 
a particular assessment type in `infoCourse.json` at the root of the repo, which looks like this:

```
{
  ...
  "assessmentSets": [
    ...
    { "abbreviation": "S", "name": "Scaffold", "heading": "Scaffold", "color": "turquoise1" }
  ],
  ...
}
```

Then, we add assessments by pushing commits to the repo to create 
  
The PrairieLearn API (documented [here](https://docs.prairielearn.com/api/#course-instances) can:

* Take a numeric course instance id, and return a list of all assessments that 
  includes both the name and the numeric assessment id (from which we can constructor the student link, which is always
  ```
  https://us.prairielearn.com/pl/course_instance/{numeric_course_instance_id}/assessment/{numeric_assessment_id}
  ```
* Take a numeric assessment id and get the following information about the assessment in an API response:
  ```
  {
    "assessment_id": "2690012",
    "assessment_name": "exam-02",
    "assessment_label": "E2",
    "type": "Exam",
    "assessment_number": "2",
    "assessment_order_by": 6,
    "title": "Final (in Testing Center)",
    "assessment_set_id": "9134429",
    "assessment_set_abbreviation": "E",
    "assessment_set_name": "Exam",
    "assessment_set_number": 8,
    "assessment_set_heading": "Exams",
    "assessment_set_color": "pink2"
  }
  ```


## User Stories

| As a(n) ... | I can... | So that... |
|-|-|-|
| Instructor | Set up c


