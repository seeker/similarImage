/@RunWith.*/d
/import org.junit.runner.RunWith/d
/import org.mockito.junit.MockitoJUnitRunner/d
/^package/a \\nimport org.junit.Rule;\nimport org.mockito.junit.MockitoRule;\nimport org.mockito.junit.MockitoJUnit;\nimport org.mockito.quality.Strictness;
/public class .*/a \\tpublic @Rule MockitoRule mockito = MockitoJUnit.rule().strictness(Strictness.STRICT_STUBS);\n