# NetLogo raytracing extension

This package contains the NetLogo raytracing extension, which provides an interface with the popular open-source POV-Ray ray tracing rendering engine.

## Using

See the documentation in the included file raytracing.html.

## Building

Use the NETLOGO environment variable to tell the Makefile where to find the NetLogo.jar to compile against.  For example:

    NETLOGO=/Applications/NetLogo\\\ 5.0beta2 make raytracing.zip

If compilation succeeds, `raytracing.jar` and `raytracing.zip` (containing everything needed to distribute the extension) should be created.


## Credits

The idea for the raytracing extension was conceived by Forrest Stonedahl, and the first version was implemented in an independent study course project by Rumou Duan at Northwestern University, under the supervision of Forrest Stonedahl and Uri Wilensky.

The raytracing extension makes use of the POV-Ray rendering engine, which must be downloaded and installed separately (see http://www.povray.org/ )

## Terms of Use

All contents © 2010-2011 Forrest Stonedahl, Rumou Duan, and Uri Wilensky.

The contents of this package may be freely copied, distributed, altered, or otherwise used by anyone for any legal purpose.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT OWNERS OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

