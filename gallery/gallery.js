const search = document.querySelector("#component-search");
const cards = [...document.querySelectorAll(".component-card")];
const emptyState = document.querySelector("#empty-state");

search.addEventListener("input", () => {
  const query = search.value.trim().toLowerCase();
  let visible = 0;

  for (const card of cards) {
    const matches = card.dataset.search.includes(query);
    card.hidden = !matches;
    if (matches) visible += 1;
  }

  emptyState.hidden = visible !== 0;
});
