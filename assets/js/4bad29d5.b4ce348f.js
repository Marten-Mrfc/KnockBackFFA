"use strict";(self.webpackChunkdocumentation=self.webpackChunkdocumentation||[]).push([[540],{8813:(e,s,n)=>{n.r(s),n.d(s,{assets:()=>c,contentTitle:()=>r,default:()=>h,frontMatter:()=>l,metadata:()=>o,toc:()=>a});const o=JSON.parse('{"id":"Getting-Started/boosts","title":"Boosts","description":"Boosts are powerful effects and abilities that enhance player gameplay in various ways. The plugin includes several types of boosts that can be:","source":"@site/docs/02-Getting-Started/05-boosts.mdx","sourceDirName":"02-Getting-Started","slug":"/Getting-Started/boosts","permalink":"/KnockBackFFA/docs/Getting-Started/boosts","draft":false,"unlisted":false,"editUrl":"https://github.com/Marten-Mrfc/KnockBackFFA/tree/master/Documentation/docs/02-Getting-Started/05-boosts.mdx","tags":[],"version":"current","sidebarPosition":5,"frontMatter":{},"sidebar":"tutorialSidebar","previous":{"title":"Item Modifiers","permalink":"/KnockBackFFA/docs/Getting-Started/modifiers"},"next":{"title":"Config settings","permalink":"/KnockBackFFA/docs/Extra-Features/config"}}');var t=n(4848),i=n(8453);const l={},r="Boosts",c={},a=[{value:"Types of Boosts",id:"types-of-boosts",level:2},{value:"Configuring Boosts",id:"configuring-boosts",level:2},{value:"Basic Structure",id:"basic-structure",level:3},{value:"Common Configuration Options",id:"common-configuration-options",level:3},{value:"Boost-Specific Options",id:"boost-specific-options",level:3},{value:"Speed Boost",id:"speed-boost",level:4},{value:"Knockback Explosion",id:"knockback-explosion",level:4},{value:"Grappling Hook",id:"grappling-hook",level:4},{value:"Adding Boosts to Kits",id:"adding-boosts-to-kits",level:2},{value:"Editing Boosts via File",id:"editing-boosts-via-file",level:2}];function d(e){const s={code:"code",h1:"h1",h2:"h2",h3:"h3",h4:"h4",header:"header",img:"img",li:"li",ol:"ol",p:"p",pre:"pre",strong:"strong",ul:"ul",...(0,i.R)(),...e.components};return(0,t.jsxs)(t.Fragment,{children:[(0,t.jsx)(s.header,{children:(0,t.jsx)(s.h1,{id:"boosts",children:"Boosts"})}),"\n",(0,t.jsx)(s.p,{children:"Boosts are powerful effects and abilities that enhance player gameplay in various ways. The plugin includes several types of boosts that can be:"}),"\n",(0,t.jsxs)(s.ul,{children:["\n",(0,t.jsx)(s.li,{children:"Given to players temporarily through shop purchases"}),"\n",(0,t.jsx)(s.li,{children:"Applied permanently as part of kit abilities"}),"\n",(0,t.jsx)(s.li,{children:"Configured to match your server's unique gameplay style"}),"\n"]}),"\n",(0,t.jsx)(s.h2,{id:"types-of-boosts",children:"Types of Boosts"}),"\n",(0,t.jsx)(s.p,{children:"The plugin includes several pre-made boosts:"}),"\n",(0,t.jsxs)(s.ul,{children:["\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Speed Boost"}),": Increases player movement speed"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Jump Boost"}),": Enhances jump height"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Knockback Resistance"}),": Reduces knockback received from attacks"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Grappling Hook"}),": Allows players to pull themselves toward any surface"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Leap"}),": Launches players forward in the direction they're facing"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Smoke Screen"}),": Creates a blinding cloud that affects nearby players"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Knockback Explosion"}),": Throwable item that knocks back nearby players"]}),"\n"]}),"\n",(0,t.jsx)(s.p,{children:"Boosts fall into two main categories:"}),"\n",(0,t.jsxs)(s.ul,{children:["\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Effect Boosts"}),": Apply potion or attribute effects to players (Speed, Jump, Resistance)"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.strong,{children:"Item Boosts"}),": Give players special items they can use (Grappling Hook, Smoke Screen)"]}),"\n"]}),"\n",(0,t.jsx)(s.h2,{id:"configuring-boosts",children:"Configuring Boosts"}),"\n",(0,t.jsxs)(s.p,{children:["All boosts can be fully customized through the ",(0,t.jsx)(s.code,{children:"boosts.yml"})," file located in your plugin directory."]}),"\n",(0,t.jsx)(s.h3,{id:"basic-structure",children:"Basic Structure"}),"\n",(0,t.jsx)(s.pre,{children:(0,t.jsx)(s.code,{className:"language-yaml",children:'boosts:\n  speed_boost:\n    enabled: true\n    name: "Speed Boost"\n    description:\n      - "Move faster around the arena"\n      - "Gain a significant speed advantage"\n    icon: RABBIT_FOOT\n    price: 30\n    speedAmplifier: 1\n    particles: false\n    ambient: true\n'})}),"\n",(0,t.jsx)(s.h3,{id:"common-configuration-options",children:"Common Configuration Options"}),"\n",(0,t.jsx)(s.p,{children:"Each boost has its own specific configuration options, but these are common to most boosts:"}),"\n",(0,t.jsxs)(s.ul,{children:["\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.code,{children:"enabled"}),": Whether the boost is available in the game"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.code,{children:"name"}),": Display name of the boost"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.code,{children:"description"}),": List of description lines shown in tooltips"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.code,{children:"icon"}),": The Material that represents this boost in menus"]}),"\n",(0,t.jsxs)(s.li,{children:[(0,t.jsx)(s.code,{children:"price"}),": Cost to purchase this boost (in coins)"]}),"\n"]}),"\n",(0,t.jsx)(s.h3,{id:"boost-specific-options",children:"Boost-Specific Options"}),"\n",(0,t.jsx)(s.p,{children:"Each boost type has unique properties you can customize:"}),"\n",(0,t.jsx)(s.h4,{id:"speed-boost",children:"Speed Boost"}),"\n",(0,t.jsx)(s.pre,{children:(0,t.jsx)(s.code,{className:"language-yaml",children:"speedAmplifier: 1  # Strength of the speed effect (0-255)\nparticles: false   # Whether to show particles\nambient: true      # Whether the effect is ambient\n"})}),"\n",(0,t.jsx)(s.h4,{id:"knockback-explosion",children:"Knockback Explosion"}),"\n",(0,t.jsx)(s.pre,{children:(0,t.jsx)(s.code,{className:"language-yaml",children:"knockbackRadius: 8.0      # Explosion radius\nknockbackStrength: 3.0    # How strongly players are thrown\nfuseTime: 3               # Seconds until explosion\nupwardKnockback: 0.3      # Vertical knockback component\n"})}),"\n",(0,t.jsx)(s.h4,{id:"grappling-hook",children:"Grappling Hook"}),"\n",(0,t.jsx)(s.pre,{children:(0,t.jsx)(s.code,{className:"language-yaml",children:"cooldown: 8           # Seconds between uses\npullStrength: 2.0     # How strongly players are pulled\nmaxYVelocity: 0.8     # Maximum upward velocity\nupwardBoost: 0.2      # Additional upward boost\n"})}),"\n",(0,t.jsx)(s.h2,{id:"adding-boosts-to-kits",children:"Adding Boosts to Kits"}),"\n",(0,t.jsx)(s.p,{children:"To add boosts to kits:"}),"\n",(0,t.jsxs)(s.ol,{children:["\n",(0,t.jsxs)(s.li,{children:["Open the Kit Editor (",(0,t.jsx)(s.code,{children:"/kbffa kit edit <kitname>"}),")"]}),"\n",(0,t.jsx)(s.li,{children:'Click the "Manage Boosts" button'}),"\n",(0,t.jsx)(s.li,{children:"Select the boosts you want to add to the kit"}),"\n",(0,t.jsxs)(s.li,{children:["Players using this kit will automatically receive these boosts\n",(0,t.jsx)(s.img,{alt:"Boost-Selector",src:n(8667).A+"",width:"1262",height:"614"})]}),"\n"]}),"\n",(0,t.jsx)(s.h2,{id:"editing-boosts-via-file",children:"Editing Boosts via File"}),"\n",(0,t.jsxs)(s.ol,{children:["\n",(0,t.jsxs)(s.li,{children:["Open ",(0,t.jsx)(s.code,{children:"boosts.yml"})," in your plugin directory"]}),"\n",(0,t.jsx)(s.li,{children:"Find the boost you want to modify"}),"\n",(0,t.jsx)(s.li,{children:"Change its values as needed"}),"\n",(0,t.jsx)(s.li,{children:"Save the file"}),"\n",(0,t.jsxs)(s.li,{children:["Use ",(0,t.jsx)(s.code,{children:"/kbffa reload"})," to apply changes"]}),"\n"]})]})}function h(e={}){const{wrapper:s}={...(0,i.R)(),...e.components};return s?(0,t.jsx)(s,{...e,children:(0,t.jsx)(d,{...e})}):d(e)}},8667:(e,s,n)=>{n.d(s,{A:()=>o});const o=n.p+"assets/images/boost-selector-d208eb03deb5fb5962b22defbf311312.png"},8453:(e,s,n)=>{n.d(s,{R:()=>l,x:()=>r});var o=n(6540);const t={},i=o.createContext(t);function l(e){const s=o.useContext(i);return o.useMemo((function(){return"function"==typeof e?e(s):{...s,...e}}),[s,e])}function r(e){let s;return s=e.disableParentContext?"function"==typeof e.components?e.components(t):e.components||t:l(e.components),o.createElement(i.Provider,{value:s},e.children)}}}]);