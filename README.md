# Library Management API

A small Spring Boot REST API (Books, Members, Loans) built as a clean, working
baseline — then used as the target for a series of documented bugs and
root-cause fixes. Part of a portfolio project demonstrating debugging and
root-cause-analysis skills after a career break.

## Tech stack
- Java 17
- Spring Boot 3.3 (Web, Data JPA, Validation)
- H2 (in-memory, local dev)
- Maven

## Status
✅ Baseline complete and verified end-to-end (checkout/return flow, validation,
error handling, duplicate/not-found/business-rule cases all tested manually
via curl/Postman). Bugs were planted and fixed on top of `v1.0` reference point in 
separate branches/commits, each with its own root-cause writeup below.

## How to run
```bash
mvn spring-boot:run
```
API available at `http://localhost:8080/api`. H2 console at `http://localhost:8080/h2-console`
(JDBC URL: `jdbc:h2:mem:librarydb`, user `sa`, blank password).

## Endpoints
- `GET/POST /api/books`, `GET/PUT/DELETE /api/books/{id}`
- `GET/POST /api/members`, `GET/DELETE /api/members/{id}`
- `POST /api/loans/checkout?bookId=&memberId=`
- `POST /api/loans/{loanId}/return`
- `GET /api/loans/member/{memberId}`

## Design decisions

**Custom exceptions + centralized handling.** All business-rule failures
(`ResourceNotFoundException` → 404, `DuplicateResourceException` → 409,
`InvalidOperationException` → 400) are mapped in one `GlobalExceptionHandler`
rather than scattered `ResponseStatusException` calls in services, so error
handling is consistent and lives in one place. Validation failures
(`@Valid`/`MethodArgumentNotValidException`) are also caught there and
reshaped into a clean `{field: message}` body instead of Spring's default
verbose error payload.

**DTOs over raw entities in API responses.** `Book`/`Member`/`Loan` have
bidirectional JPA relationships (`Book.loans` ↔ `Loan.book`, `Member.loans` ↔
`Loan.member`). Serializing entities directly caused infinite recursion in
Jackson (discovered while testing `GET /api/books/{id}`, which returned
4,000+ lines before it was traced and fixed — see Bug Log). `BookDTO`,
`MemberDTO`, and `LoanDTO` (implemented as Java records) break the cycle:
`LoanDTO` holds only `bookId`/`memberId` (not nested `BookDTO`/`MemberDTO`
objects), so `BookDTO`/`MemberDTO` can safely embed `List<LoanDTO>` without
reopening the loop. Conversion happens via a `fromEntity()` static factory on
each DTO; services still operate on real entities internally, and the
controller layer converts to DTOs at the response boundary.

**A redundant-looking safety check that was deliberately removed.**
`LoanService.returnBook()` originally checked both `loan.getReturnDate() !=
null` *and* `book.getTotalCopies() == book.getAvailableCopies()` before
allowing a return. The second check turned out to be unreachable given
consistent data — if `returnDate` is null, at least one copy is provably
still out, so the counts check can never independently catch anything the
first check doesn't. It's commented out in the code with an explanation
rather than deleted outright, since this kind of defensive check is one I've
added in real projects before: in shared dev/QA/UAT environments, people
sometimes edit data directly in the database, and a check like this can catch
the resulting drift. For this project the check was judged unnecessary, but
it's a real tradeoff (defensive coding vs. dead-code clarity) worth being
explicit about rather than silently dropping.

## Bug log

### Found and fixed during baseline development
These weren't planted — they were real bugs hit while building and manually
testing the baseline, before `v1.0` was tagged.

- **Infinite JSON recursion on `GET /api/books/{id}` and similar endpoints.**
  Bidirectional JPA relationships between `Book`/`Member` and `Loan` caused
  Jackson to serialize in an endless loop (`Book` → `loans` → `Loan` → `book`
  → `loans` → ...), producing a multi-thousand-line response instead of
  erroring outright. Root cause: entities were being returned directly from
  controllers. Fix: introduced DTOs (see Design decisions above) so the API
  never serializes a full entity graph.
- **Validation failures returned Spring's raw internal error format**
  (full exception class names, internal error codes) instead of a clean,
  consistent error body. Fix: added a `MethodArgumentNotValidException`
  handler that extracts just the field name and message into a plain map.

### Found and fixed after the v1.0 baseline
- **Fixed: delete on a non-existent ID returned 204 instead of 404.**
  deleteById() in Spring Data JPA no-ops if the ID doesn't exist,
  rather than throwing — so DELETE /api/books/99999 returned a false success.
  Found by testing delete against a known-invalid ID and inspecting the response,
  not by reading an exception trace. Fixed by checking existsById() before calling
  deleteById(), throwing ResourceNotFoundException when missing.
- **Fixed: N+1 query on GET /api/books.**
  Added a JOIN FETCH query (findAllWithLoans())
  to BookRepository, replacing the default findAll() in getAllBooks(). This fetches
  all books and their associated loans in a single query instead of one query per book —
  confirmed by re-running the same test: the 3-query SQL log from before dropped to a
  single query.
- **Fixed: no validation on Book copy counts.**
  Added @Min(0) to both totalCopies and
  availableCopies on the Book entity. Retesting the same request now returns a clean 400
  with a field-level message ("must be greater than or equal to 0") instead of silently
  accepting invalid data.
- **Fixed: updateBook() didn't check ISBN uniqueness.**
  createBook() validates ISBN uniqueness before saving; updateBook() didn't have the
  equivalent check, so updating a book's ISBN to one already in use hit the raw H2 unique-
  constraint violation — returning a generic 500 Internal Server Error with no useful message.
  Fixed two ways: (1) added an existsByIsbn check in updateBook() giving a precise 409
  "ISBN already exists" error; (2) added a DataIntegrityViolationException handler in
  GlobalExceptionHandler as a backstop for the residual race-condition window between the
  check and the save, where two concurrent updates could both pass the check before either
  commits. The two exist for different reasons — the check protects the client-facing error
  message, the handler protects against the write-time race the check alone can't fully close.

### Planted and fixed bugs 
- **Fixed: missing transaction boundary in checkoutBook().** 
  Restored @Transactional on the method. The Book update and Loan creation now happen 
  inside a single database transaction — either both commit or both roll back, so the 
  two writes can never drift out of sync. (Not caught by reproducing an actual crash 
  — hard to force deterministically — but identified by tracing the method and asking 
  "what happens if this fails halfway through?")
 