SipHash implementation with hand inlining the SIPROUND

To know details about SipHash, see;
"a fast short-input PRF" https://www.131002.net/siphash/

SIPROUND is defined in siphash24.c that can be downloaded from the above site.
Following license notice is subject to change based on the licensing policy of
siphash24.c.

Copyright 2012  Hiroshi Nakamura <nahi@ruby-lang.org>

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
