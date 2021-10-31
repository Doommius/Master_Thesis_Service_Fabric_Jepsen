// ------------------------------------------------------------
//  Copyright (c) Microsoft Corporation.  All rights reserved.
//  Licensed under the MIT License (MIT). See License.txt in the repo root for license information.
// ------------------------------------------------------------

namespace ReliableCollectionsWebAPI.Controllers
{
    using System.Collections.Generic;
    using System.Threading;
    using System.Threading.Tasks;
    using Microsoft.AspNetCore.Mvc;
    using Microsoft.ServiceFabric.Data;
    using Microsoft.ServiceFabric.Data.Collections;
    using System.Net.Http;
    using System;
    using System.Fabric;
    using Newtonsoft.Json.Linq;

    [Route("api/[controller]")]
    public class ReliableDictionaryController : Controller
    {
        private readonly IReliableStateManager stateManager;

        public ReliableDictionaryController(IReliableStateManager stateManager)
        {
            this.stateManager = stateManager;
        }

        public System.Fabric.ReplicaRole ReplicaRole { get; }


        [HttpGet]
        public async Task<IActionResult> Get()
        {
            CancellationToken ct = new CancellationToken();

            IReliableDictionary<string, int> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, int>>("counts");

            using (ITransaction tx = this.stateManager.CreateTransaction())
            {
                var list = await votesDictionary.CreateEnumerableAsync(tx);

                var enumerator = list.GetAsyncEnumerator();

                List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();

                while (await enumerator.MoveNextAsync(ct))
                {
                    result.Add(enumerator.Current);
                }

                return this.Json(result);
            }
        }

        // GET VoteData/name
        [HttpGet("{key}")]
        public async Task<IActionResult> Get(string key)
        {

        
            IReliableDictionary<string, int> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, int>>("counts");
            List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();


            using (ITransaction tx = this.stateManager.CreateTransaction())
            {
                ConditionalValue<int> conditionalValue = await votesDictionary.TryGetValueAsync(tx, key);
                if (conditionalValue.HasValue)
                {
                    int value = conditionalValue.Value;
                    result.Add(new KeyValuePair<string, int>(key, value));

                    return this.Json(result);
                }
                else
                {
                    return NoContent();
                }

            }
        }

        [HttpPut("{key}")]
        public async Task<IActionResult> Put(string key)
        {
            IReliableDictionary<string, int> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, int>>("counts");

            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    await votesDictionary.AddOrUpdateAsync(tx, key, 1, (key, oldvalue) => oldvalue + 1);
                    await tx.CommitAsync();
                }

                return new OkResult();
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }

        }

        [HttpPut]
        public async Task<IActionResult> Put()
        {
            List<KeyValuePair<string, string>> result = new List<KeyValuePair<string, string>>();
            try

            {
                IReliableDictionary<string, List<int>> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, List<int>>>("counts");

                String transactionquery;
                if (!String.IsNullOrEmpty(HttpContext.Request.Query["query"]))
                {
                    transactionquery = HttpContext.Request.Query["query"];
                }

                else
                {
                    return NoContent();
                }

                
                dynamic operationlist;

                operationlist = Newtonsoft.Json.JsonConvert.DeserializeObject(transactionquery);

                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    ConditionalValue<int> conditionalValue;
                    ConditionalValue<Task> conditionalTask;
                    Boolean v;
                    foreach (var item in operationlist.transaction)
                    {

                        if (item.operation.Value == "r")
                        {
                            conditionalValue = await votesDictionary.TryGetValueAsync(tx, item.key.Value);
                            if (conditionalValue.HasValue)
                            {
                                int value = conditionalValue.Value;
                                result.Add(new KeyValuePair<string, string>(item.key.Value, value.ToString()));

                            }
                            else
                            {
                                result.Add(new KeyValuePair<string, string>(item.key.Value, "False"));
                            }
                        }

                        else if (item.operation.Value == "w")
                        {

                            int returnint = await votesDictionary.SetAsync(tx, item.key.Value, new List<int>() { (int)item.value.Value });
                            result.Add(new KeyValuePair<string, string>(item.key.Value, returnint.ToString()));
                        }
                        else if (item.operation.Value == "a")
                        {

                            int returnint = await votesDictionary.TryAddAsync(tx, item.key.Value, new List<int>() { (int)item.value.Value });
                            result.Add(new KeyValuePair<string, string>(item.key.Value, returnint.ToString()));
                        }
                        else if (item.operation.Value == "c")
                        {
                            v = await votesDictionary.TryUpdateAsync(tx, item.key.Value, new List<int>() { (int)item.value.Value }, new List<int>(){(int)item.expected.Value});
                            if (!v)
                            {
                                result.Add(new KeyValuePair<string, string>(item.key.Value, "False"));
                            }
                            else
                            {
                                result.Add(new KeyValuePair<string, string>(item.key.Value, "True"));
                            }

                        }
                        else if (item.operation.Value == "abort")
                        {

                            tx.Abort();
                            result.Add(new KeyValuePair<string, string>(item.operation.Value, "abort"));
                            return this.Json(result);
                        }
                        else if (item.operation.Value == "d")
                        {
                            conditionalValue = await votesDictionary.TryRemoveAsync(tx, item.key.Value);
                            if (conditionalValue.HasValue)
                                result.Add(new KeyValuePair<string, string>(item.key.Value, "True"));
                            else
                            {
                                result.Add(new KeyValuePair<string, string>(item.key.Value, "False"));
                            }
                        }
                        else
                        {
                            result.Add(new KeyValuePair<string, string>(item.operation.Value, "Failed"));
                        }
                    }
                 await tx.CommitAsync();
                }
                return this.Json(result);
            }
            catch (Exception e)
            {
                result.Add(new KeyValuePair<string, string>("Exception", e.ToString()));
                return this.Json(result);
            }
        }



        [HttpPut("{key}/{value}")]
        public async Task<IActionResult> Put(string key, int value)
        {
            IReliableDictionary<string, int> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, int>>("counts");
            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    await votesDictionary.AddOrUpdateAsync(tx, key, value, (key, oldvalue) => value);
                    await tx.CommitAsync();
                }

                List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();
                result.Add(new KeyValuePair<string, int>(key, value));
                return this.Json(result);
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }


        }


        // POST VoteData/cas
        [HttpPut("{key}/{value}/{expected}")]
        public async Task<IActionResult> put(string key, int value, int expected)
        {
            IReliableDictionary<string, int> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, int>>("counts");
            List<KeyValuePair<string, int>> result = new List<KeyValuePair<string, int>>();

            Boolean v;
            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    v = await votesDictionary.TryUpdateAsync(tx, key, value, expected);
                    await tx.CommitAsync();
                }
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }
 
            if (!v)
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    ConditionalValue<int> conditionalValue = await votesDictionary.TryGetValueAsync(tx, key);
                    value = conditionalValue.Value;
                }

            }

            result.Add(new KeyValuePair<string, int>(key, value));
            return this.Json(result);

        }


        // DELETE VoteData/name
        [HttpDelete("{key}")]
        public async Task<IActionResult> Delete(string key)
        {
            IReliableDictionary<string, int> votesDictionary = await this.stateManager.GetOrAddAsync<IReliableDictionary<string, int>>("counts");
            try
            {
                using (ITransaction tx = this.stateManager.CreateTransaction())
                {
                    if (await votesDictionary.ContainsKeyAsync(tx, key))
                    {
                        await votesDictionary.TryRemoveAsync(tx, key);
                        await tx.CommitAsync();
                        return new OkResult();
                    }
                    else
                    {
                        return new NotFoundResult();
                    }
                }
            }
            catch (FabricNotPrimaryException)
            {
                return new ForbidResult("Not Primary");
            }

        }
    }
}