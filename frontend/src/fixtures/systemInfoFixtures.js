const systemInfoFixtures = {
  showingBoth: {
    springH2ConsoleEnabled: true,
    showSwaggerUILink: true,
    oauthLogin: "/oauth2/authorization/google",
    sourceRepo: "https://github.com/ucsb-cs156/proj-scaffold",
  },
  showingNeither: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
    oauthLogin: "/oauth2/authorization/google",
    sourceRepo: "https://github.com/ucsb-cs156/proj-scaffold",
  },
  oauthLoginUndefined: {
    springH2ConsoleEnabled: false,
    showSwaggerUILink: false,
    sourceRepo: "https://github.com/ucsb-cs156/proj-scaffold",
  },
  initialData: {
    initialData: true,
    springH2ConsoleEnabled: true,
    showSwaggerUILink: true,
    oauthLogin: "/oauth2/authorization/google",
    sourceRepo: "https://github.com/ucsb-cs156/proj-scaffold",
  },
};

export { systemInfoFixtures };
