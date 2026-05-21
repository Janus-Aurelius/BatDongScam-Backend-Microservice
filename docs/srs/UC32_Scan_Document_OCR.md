### UC32: Scan Document with OCR
**Name**: Scan Document with OCR
**Description**: This use case describes the automated extraction of text from identity or land documents to facilitate form completion.
**Actor**: User
**Trigger**: ❖ When the user uploads a document image to an OCR-enabled field.
**Pre-condition**: 
❖ The user is on a registration or verification page.
**Post-condition**: 
❖ Extracted text fields are populated in the UI form.

**Activities Flow (PlantUML)**:

```plantuml
@startuml
|User|
start
: (1) Upload document image (JPG/PNG);
|System|
: (2) Process image via OCR engine;
if (Quality OK?) then (yes)
  : (3) Parse fields using regex and NLP;
  : (4) Calculate confidence scores;
  if (Confidence > 0.8?) then (yes)
    |User|
    : (5) Auto-fill form and show success message MSG 41;
    stop
  else (no)
    |User|
    : (6) Highlight fields and show message MSG 42;
    stop
  endif
else (no)
  |User|
  : (7) Show error message MSG 11;
  stop
endif
@enduml
```

```plantuml
@startuml
actor User
boundary "Verification UI" as UI
control "OcrController" as API
control "OcrService" as SVC
control "OCR_Gateway" as AI

User -> UI : Upload Image
UI -> API : POST /ocr/scan (File)
API -> SVC : scanDocument(file)
SVC -> AI : (BR92) POST /detect_text (Image)
AI --> SVC : [rawText, boundingBoxes]
SVC -> SVC : (BR93) Extract fields [name, id_number, address] using pattern.compile()
SVC -> SVC : Check field confidence
alt Confidence > 0.8
    SVC --> API : OCRDataDTO
    API --> UI : 200 OK
    UI --> User : Auto-fill Form & MSG 41
else Low Confidence
    SVC --> API : throw LowConfidenceException
    API --> UI : 200 OK (Partial Data)
    UI --> User : Populate Fields & MSG 42
end
@enduml
```

**Business Rules**:

| Activity | BR Code | Description |
| :--- | :--- | :--- |
| (2) | BR92 | **OCR Rules:**<br>❖ [ocrResult] = OCR Gateway scan([image]).<br>❖ If [image.size] > 10.MB then return error message MSG 11. |
| (3) | BR93 | **Parsing Rules:**<br>❖ [idNumber] = pattern.compile("^[0-9]{12}$").find([ocrResult.rawText]).<br>❖ [name] = pattern.compile("[A-Z ]+").find([ocrResult.rawText]).<br>❖ [fields] = Map of extracted items with confidence scores. |
| (5) | BR41 | **Message Rules:**<br>❖ The system shows success message MSG 41 ("Document scanned successfully"). |
| (6) | BR42 | **Message Rules:**<br>❖ The system shows informational message MSG 42 ("Low confidence detected, please verify fields manually"). |
